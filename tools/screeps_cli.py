#!/usr/bin/env python3
"""Live Screeps game console using python-screeps and gradle.properties."""
from __future__ import annotations

import argparse
import asyncio
import base64
import html
import json
import math
import queue
import re
import sys
import threading
import time
import uuid
import zlib
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from urllib.parse import urlsplit

try:
    import javaproperties
    from prompt_toolkit.application import Application
    from prompt_toolkit.auto_suggest import AutoSuggestFromHistory
    from prompt_toolkit.document import Document
    from prompt_toolkit.key_binding import KeyBindings
    from prompt_toolkit.layout import HSplit, Layout, Window
    from prompt_toolkit.layout.controls import FormattedTextControl
    from prompt_toolkit.widgets import TextArea
    import requests
    from screepsapi import API
    import websocket
except ImportError as exc:
    raise SystemExit(
        "Missing CLI dependency. Install with: python -m pip install -r tools/requirements.txt"
    ) from exc

ROOT = Path(__file__).resolve().parent.parent


class ConsoleError(Exception):
    """A user-facing configuration, transport, or API failure."""


class ApiError(ConsoleError):
    def __init__(self, message: str, status: int = 0):
        super().__init__(message)
        self.status = status


@dataclass(frozen=True)
class Settings:
    host: str
    user: str | None = None
    password: str | None = field(default=None, repr=False)
    token: str | None = field(default=None, repr=False)
    shard: str | None = None
    server_password: str | None = field(default=None, repr=False)

    @classmethod
    def load(cls, path: Path, shard: str | None = None) -> Settings:
        try:
            props = javaproperties.loads(path.read_text(encoding="utf-8-sig"))
        except (OSError, ValueError) as exc:
            raise ConsoleError(f"Cannot read properties file: {path}") from exc
        host = props.get("screepsHost", "https://screeps.com").strip().rstrip("/")
        url = urlsplit(host)
        if (url.scheme not in ("http", "https") or not url.hostname or url.username
                or url.password or url.query or url.fragment):
            raise ConsoleError("screepsHost must be an http(s) URL with optional port and path prefix")
        user = props.get("screepsUser") or None
        password = props.get("screepsPassword") or None
        token = props.get("screepsToken", "").strip() or None
        if not token and not (user and password):
            raise ConsoleError("Set screepsToken or both screepsUser and screepsPassword in gradle.properties")
        selected_shard = shard if shard is not None else props.get("screepsShard")
        if selected_shard is None and url.hostname == "screeps.com":
            selected_shard = "shard0"
        return cls(host, user, password, token, selected_shard or None,
                   props.get("screepsServerPassword") or None)

    @property
    def socket_url(self) -> str:
        return self.host.replace("https://", "wss://", 1).replace("http://", "ws://", 1) + "/socket/websocket"


class ScreepsClient(API):
    """Keep python-screeps endpoints, but bound I/O and synchronize token rotation."""

    def __init__(self, settings: Settings):
        self.settings = settings
        self.lock = threading.RLock()
        self.secret_lock = threading.Lock()
        self.http = requests.Session()
        self.secrets = {s for s in (settings.password, settings.token, settings.server_password) if s}
        self.session_token = not bool(settings.token)
        self.token_updated = 0.0
        self.base_url = settings.host + "/api/"
        url = urlsplit(settings.host)
        super().__init__(token=settings.token, host=url.netloc, secure=url.scheme == "https")

    def redact(self, text: object) -> str:
        with self.secret_lock:
            result = str(text)
            for secret in sorted(self.secrets, key=len, reverse=True):
                result = result.replace(secret, "[REDACTED]")
            return result

    def update_token(self, token: str) -> None:
        with self.lock:
            self.token = token
            self.token_updated = time.monotonic()
            with self.secret_lock:
                self.secrets.add(token)

    def _login(self) -> None:
        if not (self.settings.user and self.settings.password):
            raise ConsoleError(
                "Authentication expired or was rejected; configure screepsUser/screepsPassword or a valid token")
        result = self.post("auth/signin", email=self.settings.user, password=self.settings.password)
        token = result.get("token")
        if not isinstance(token, str) or not token:
            raise ConsoleError("Sign-in did not return a token; private password login requires screepsmod-auth")
        self.session_token = True
        self.update_token(token)

    def req(self, func, path, **args):
        with self.lock:
            if path != "auth/signin" and (not self.token or
                                          (self.session_token and time.monotonic() - self.token_updated > 40)):
                self._login()
            try:
                return self._send(func, path, **args)
            except ApiError as exc:
                # A 401 rejects authentication before executing a console command.
                # Never retry a POST on a timeout: it may already have been accepted.
                if path == "auth/signin" or exc.status != 401 or not (self.settings.user and self.settings.password):
                    raise
                self._login()
                return self._send(func, path, **args)

    def _send(self, func, path, **args):
        headers = {}
        if self.token:
            headers.update({"X-Token": self.token, "X-Username": self.token})
        if self.settings.server_password:
            headers["X-Server-Password"] = self.settings.server_password
        try:
            response = self.http.request(func.__name__.upper(), self.base_url + path,
                                         headers=headers, timeout=(5, 15), **args)
        except requests.Timeout as exc:
            raise ConsoleError("API request timed out; command delivery is unknown, so it was not retried") from exc
        except requests.RequestException as exc:
            raise ConsoleError(f"Cannot connect to Screeps API ({type(exc).__name__})") from exc
        if not response.ok:
            raise ApiError(f"API {path}: HTTP {response.status_code}", response.status_code)
        try:
            data = response.json()
        except ValueError as exc:
            raise ConsoleError(f"API {path} returned invalid JSON") from exc
        if not isinstance(data, dict):
            raise ConsoleError(f"API {path} returned an unexpected response")
        if data.get("error") or data.get("ok") == 0:
            # Do not print raw server responses: sign-in responses can contain secrets.
            raise ApiError(f"API {path} rejected the request")
        if response.headers.get("X-Token"):
            self.update_token(response.headers["X-Token"])
        return data

    def identity(self) -> str:
        result = self.me()
        user_id = result.get("_id")
        if not isinstance(user_id, str) or not user_id:
            raise ConsoleError("API auth/me did not return a user id")
        return user_id

    def execute(self, expression: str) -> None:
        self.console(expression, shard=self.settings.shard)


def console_lines(message: str, shard: str | None = None) -> list[tuple[str, str]]:
    """Decode the raw Screeps socket protocol, including optional gz: frames."""
    if message.startswith("gz:"):
        message = zlib.decompress(base64.b64decode(message[3:])).decode("utf-8")
    if not message.startswith("["):
        return []
    event = json.loads(message)
    if not isinstance(event, list) or len(event) != 2:
        return []
    channel, payload = event
    if not isinstance(channel, str) or not channel.endswith("/console") or not isinstance(payload, dict):
        return []
    if shard and payload.get("shard") not in (None, shard):
        return []
    lines = []
    messages = payload.get("messages", {})
    if isinstance(messages, dict):
        for key, label in (("log", "log"), ("results", "result")):
            values = messages.get(key, [])
            for value in values if isinstance(values, list) else [values]:
                lines.append((label, str(value)))
    if payload.get("error"):
        lines.append(("error", str(payload["error"])))
    return lines


def plain_text(text: str) -> str:
    # The game console often wraps colored text in HTML, which terminals cannot render.
    text = re.sub(r"</?(?:span|font|a|b|i|br)\b[^>]*>", "", text, flags=re.IGNORECASE)
    text = html.unescape(text)
    text = re.sub(r"\x1b\[[0-?]*[ -/]*[@-~]", "", text)
    return "".join(c for c in text if c in "\n\t" or (ord(c) >= 32 and ord(c) != 127))


def configure_output() -> None:
    """Configure real stdio before prompt_toolkit captures it, including Windows pipes."""
    for output in (sys.stdout, sys.stderr):
        reconfigure = getattr(output, "reconfigure", None)
        if reconfigure is not None:
            reconfigure(encoding="utf-8", errors="backslashreplace")


def write_line(text: str, *, stream=None) -> None:
    """Embedded/IDE streams may not support reconfigure; escape only unsupported chars."""
    output = stream if stream is not None else sys.stdout
    try:
        output.write(text + "\n")
        output.flush()
    except UnicodeEncodeError:
        encoding = getattr(output, "encoding", None) or "ascii"
        escaped = text.encode(encoding, errors="backslashreplace").decode(encoding)
        output.write(escaped + "\n")
        output.flush()


class ConsoleStream:
    def __init__(self, client: ScreepsClient, emit):
        self.client = client
        self.emit = emit
        self.stop_event = threading.Event()
        self.ready = threading.Event()
        self.socket = None
        self.thread = threading.Thread(target=self._run, name="screeps-console", daemon=True)

    def start(self) -> None:
        self.thread.start()

    def stop(self) -> None:
        self.stop_event.set()
        if self.socket:
            self.socket.close()
        self.thread.join(timeout=3)

    def _run(self) -> None:
        delay = 1
        while not self.stop_event.is_set():
            try:
                self._connect()
                delay = 1
            except (ConsoleError, websocket.WebSocketException, OSError) as exc:
                if self.ready.is_set():
                    delay = 1
                if not self.stop_event.is_set():
                    self.emit("status", f"Stream disconnected: {type(exc).__name__}; retry in {delay}s")
            finally:
                self.ready.clear()
                if self.socket:
                    self.socket.close()
                    self.socket = None
            if self.stop_event.wait(delay):
                return
            delay = min(delay * 2, 15)

    def _connect(self) -> None:
        user_id = self.client.identity()
        headers = []
        if self.client.settings.server_password:
            headers.append("X-Server-Password: " + self.client.settings.server_password)
        self.socket = websocket.create_connection(self.client.settings.socket_url,
                                                  timeout=5, header=headers)
        self.socket.settimeout(1)
        with self.client.lock:
            self.socket.send("auth " + self.client.token)
        deadline = time.monotonic() + 10
        while not self.stop_event.is_set():
            if not self.ready.is_set() and time.monotonic() > deadline:
                raise ConsoleError("Socket authentication timed out")
            try:
                message = self.socket.recv()
            except websocket.WebSocketTimeoutException:
                continue
            if not message:
                raise ConsoleError("Socket closed")
            if isinstance(message, bytes):
                message = message.decode("utf-8")
            if message.startswith("auth ok"):
                token = message.removeprefix("auth ok").strip()
                if token:
                    self.client.update_token(token)
                self.socket.send("subscribe user:" + user_id + "/console")
                self.ready.set()
                self.emit("status", "Console stream connected")
            elif message.startswith("auth failed"):
                # With credentials, obtain a fresh token before the next connection.
                with self.client.lock:
                    if self.client.settings.user and self.client.settings.password:
                        self.client.token = None
                raise ConsoleError("Socket authentication rejected")
            else:
                try:
                    lines = console_lines(message, self.client.settings.shard)
                except (ValueError, zlib.error):
                    self.emit("error", "Invalid console frame; skipped")
                    continue
                for kind, line in lines:
                    self.emit(kind, line)


class TerminalConsole:
    """A fixed input row; only the UI event loop modifies screen buffers."""
    MAX_LOG_LINES = 2000

    def __init__(self, execute, can_send, emit, *, input=None, output=None):
        self.execute = execute
        self.can_send = can_send
        self.emit = emit
        self.pending = queue.SimpleQueue()
        self.lines = deque(maxlen=self.MAX_LOG_LINES)
        self.commands = queue.Queue(maxsize=16)
        self.stopped = threading.Event()
        self.worker = threading.Thread(target=self._commands, name="screeps-commands", daemon=True)
        self.logs = TextArea(read_only=True, focusable=False, scrollbar=True, wrap_lines=True)
        self.input = TextArea(height=1, multiline=False, wrap_lines=False, prompt="screeps> ",
                              auto_suggest=AutoSuggestFromHistory(), accept_handler=self._accept)
        bindings = KeyBindings()

        @bindings.add("c-c")
        def clear_input(event):
            self.input.buffer.reset()

        @bindings.add("c-d")
        def quit_console(event):
            event.app.exit()

        @bindings.add("c-l")
        def clear_logs(event):
            self.clear()

        self.app = Application(
            layout=Layout(HSplit([
                self.logs,
                Window(FormattedTextControl(" Enter: send | Ctrl+C: clear input | Ctrl+D: quit | Ctrl+L: clear logs "),
                       height=1, style="reverse"),
                self.input,
            ]), focused_element=self.input),
            key_bindings=bindings, full_screen=True, min_redraw_interval=0.05,
            input=input, output=output,
        )

    def post(self, line: str) -> None:
        self.pending.put(line)

    def clear(self) -> None:
        self.pending.put(None)

    def _flush_pending(self) -> None:
        changed = False
        for _ in range(500):
            try:
                line = self.pending.get_nowait()
            except queue.Empty:
                break
            if line is None:
                self.lines.clear()
            else:
                self.lines.append(line)
            changed = True
        if changed:
            text = "\n".join(self.lines)
            self.logs.document = Document(text, cursor_position=len(text))
            self.app.invalidate()

    async def _refresh(self) -> None:
        while True:
            self._flush_pending()
            await asyncio.sleep(0.05)

    def _accept(self, buffer):
        expression = buffer.text.strip()
        if not expression:
            return False
        if expression in ("/quit", "/exit"):
            self.app.exit()
        elif expression == "/help":
            self.emit("status", "/help /clear /quit; Enter sends JavaScript; Ctrl+C clears input; Ctrl+D exits")
        elif expression == "/clear":
            self.clear()
        elif expression.startswith("/"):
            self.emit("error", "Unknown local command; use /help")
        elif not self.can_send():
            self.emit("error", "Stream is reconnecting; wait before sending a command")
            return True  # Preserve the command for resubmission after reconnecting.
        else:
            try:
                self.commands.put_nowait(expression)
            except queue.Full:
                self.emit("error", "Command queue is full; wait for pending requests")
                return True
            self.emit("command", expression)
        return False

    def _commands(self) -> None:
        while not self.stopped.is_set():
            try:
                expression = self.commands.get(timeout=0.1)
            except queue.Empty:
                continue
            if self.stopped.is_set():
                return
            try:
                self.execute(expression)
            except ConsoleError as exc:
                self.emit("error", exc)

    def start(self) -> None:
        self.worker.start()
        self.app.create_background_task(self._refresh())

    def stop(self) -> None:
        self.stopped.set()
        if self.worker.ident is not None:
            self.worker.join(timeout=1)

    def run(self) -> None:
        try:
            self.app.run(pre_run=self.start)
        finally:
            self.stop()


def run(settings: Settings, *, logs_only: bool = False, check: bool = False) -> int:
    configure_output()
    client = ScreepsClient(settings)
    interactive = not (logs_only or check) and sys.stdin.isatty() and sys.stdout.isatty()
    terminal = None
    startup_lines = deque(maxlen=TerminalConsole.MAX_LOG_LINES)

    def emit(kind, text):
        timestamp = datetime.now().strftime("%H:%M:%S")
        for line in client.redact(plain_text(client.redact(text))).splitlines():
            formatted = f"[{timestamp}] {kind:>6} | {line}"
            if terminal is not None:
                terminal.post(formatted)
            else:
                if interactive:
                    startup_lines.append(formatted)
                write_line(formatted)

    stream = ConsoleStream(client, emit)
    try:
        # Fail fast on invalid initial credentials instead of silently reconnecting forever.
        client.identity()
        emit("status", f"Connecting to {settings.host}; shard={settings.shard or 'private/default'}")
        stream.start()
        if not stream.ready.wait(15):
            raise ConsoleError("Console stream did not authenticate within 15 seconds")
        if check:
            emit("status", "Connection check passed; no game commands sent")
            return 0
        if logs_only:
            while not stream.stop_event.wait(0.5):
                pass
        elif interactive:
            terminal = TerminalConsole(client.execute, stream.ready.is_set, emit)
            for line in startup_lines:
                terminal.post(line)
            startup_lines.clear()
            terminal.run()
        else:
            emit("status", "Plain input mode; use a terminal (Windows Terminal/PowerShell) for a fixed input row")
            while True:
                try:
                    expression = input("screeps> ").strip()
                except KeyboardInterrupt:
                    continue
                except EOFError:
                    break
                if not expression:
                    continue
                if expression in ("/quit", "/exit"):
                    break
                if expression == "/help":
                    emit("status", "/help /quit; Enter sends JavaScript; Ctrl+D exits")
                elif expression == "/clear":
                    pass
                elif expression.startswith("/"):
                    emit("error", "Unknown local command; use /help")
                elif not stream.ready.is_set():
                    emit("error", "Stream is reconnecting; wait before sending a command")
                else:
                    try:
                        client.execute(expression)
                    except ConsoleError as exc:
                        emit("error", exc)
    except KeyboardInterrupt:
        pass
    except ConsoleError as exc:
        emit("error", exc)
        return 1
    finally:
        if terminal is not None:
            terminal.stop()
        if stream.thread.ident is not None:
            stream.stop()
        client.http.close()
    return 0


def make_run_command(expression: str, marker: str) -> str:
    # Console events have no request id. Return an envelope with a per-invocation
    # marker so another terminal's result or an ordinary log cannot satisfy run.
    template = r"""(function() {
        try {
            var value = (0, eval)(__EXPRESSION__);
            var seen = [];
            var rendered = typeof value === 'string' ? value : JSON.stringify(value, function(key, item) {
                if (typeof item === 'bigint') return String(item);
                if (item !== null && typeof item === 'object') {
                    if (seen.indexOf(item) !== -1) return '[Circular]';
                    seen.push(item);
                }
                return item;
            }, 2);
            if (rendered === undefined) rendered = String(value);
            return __MARKER__ + JSON.stringify({ok: true, result: rendered});
        } catch (error) {
            return __MARKER__ + JSON.stringify({ok: false, error: String(error && error.stack || error)});
        }
    })()"""
    template = "".join(line.strip() for line in template.splitlines())
    # Substitute the expression last: its contents must never be treated as template tokens.
    return template.replace("__MARKER__", json.dumps(marker)).replace("__EXPRESSION__", json.dumps(expression))


def run_once(settings: Settings, expression: str, *, timeout: float = 30) -> int:
    configure_output()
    if not expression.strip():
        write_line("Error: expression must not be empty", stream=sys.stderr)
        return 1
    client = ScreepsClient(settings)
    marker = "__screeps_cli_" + uuid.uuid4().hex + "__:"
    responses = queue.SimpleQueue()

    def receive(kind, text):
        if kind != "result" or not text.startswith(marker):
            return
        try:
            response = json.loads(text[len(marker):])
        except ValueError:
            response = None
        responses.put(response)

    stream = ConsoleStream(client, receive)
    try:
        client.identity()
        stream.start()
        if not stream.ready.wait(15):
            raise ConsoleError("Console stream did not authenticate within 15 seconds")
        client.execute(make_run_command(expression, marker))
        try:
            response = responses.get(timeout=timeout)
        except queue.Empty:
            write_line("Error: timed out waiting for the command result; the command was submitted and was not resent. "
                       "A paused server must resume ticking to produce a result.", stream=sys.stderr)
            return 124
        if not isinstance(response, dict) or not isinstance(response.get("ok"), bool):
            raise ConsoleError("Invalid command result envelope")
        if not response["ok"]:
            write_line(client.redact(str(response.get("error", "JavaScript execution failed"))), stream=sys.stderr)
            return 1
        result = response.get("result")
        if not isinstance(result, str):
            raise ConsoleError("Invalid command result value")
        write_line(client.redact(result))
        return 0
    except ConsoleError as exc:
        write_line("Error: " + client.redact(exc), stream=sys.stderr)
        return 1
    except KeyboardInterrupt:
        return 130
    finally:
        if stream.thread.ident is not None:
            stream.stop()
        client.http.close()


def positive_timeout(value: str) -> float:
    try:
        result = float(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("timeout must be a positive finite number") from exc
    if not math.isfinite(result) or result <= 0:
        raise argparse.ArgumentTypeError("timeout must be a positive finite number")
    return result


def main(argv=None) -> int:
    configure_output()
    connection = argparse.ArgumentParser(add_help=False)
    connection.add_argument("--properties", type=Path, default=argparse.SUPPRESS)
    connection.add_argument("--shard", default=argparse.SUPPRESS, help="Shard override")
    parser = argparse.ArgumentParser(description=__doc__, parents=[connection])
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--logs-only", action="store_true", help="Follow logs without an input prompt")
    mode.add_argument("--check", action="store_true",
                      help="Check API and socket login, then exit without executing commands")
    commands = parser.add_subparsers(dest="command")
    once = commands.add_parser("run", parents=[connection], help="Execute JavaScript, print its result, and exit")
    once.add_argument("expression", help="JavaScript expression or statements in quotes")
    once.add_argument("--timeout", type=positive_timeout, default=30, metavar="SECONDS",
                      help="Result wait timeout after submission (default: 30)")
    args = parser.parse_args(argv)
    if args.command and (args.logs_only or args.check):
        parser.error("run cannot be combined with --logs-only or --check")
    try:
        settings = Settings.load(getattr(args, "properties", ROOT / "gradle.properties"), getattr(args, "shard", None))
    except ConsoleError as exc:
        write_line(f"Error: {exc}", stream=sys.stderr)
        return 1
    if args.command == "run":
        return run_once(settings, args.expression, timeout=args.timeout)
    return run(settings, logs_only=args.logs_only, check=args.check)


if __name__ == "__main__":
    raise SystemExit(main())
