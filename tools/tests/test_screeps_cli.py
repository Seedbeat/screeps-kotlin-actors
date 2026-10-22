"""Protocol tests use a local HTTP/WebSocket server, never gradle.properties."""
import asyncio
import base64
import hashlib
import io
import json
import shutil
import socket
import struct
import subprocess
import sys
import tempfile
import threading
import time
import unittest
import zlib
from contextlib import redirect_stdout, redirect_stderr
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from screeps_cli import ConsoleError, ConsoleStream, ScreepsClient, Settings, console_lines, plain_text, run, \
    configure_output, write_line, TerminalConsole, run_once, make_run_command, main
import requests


class SettingsTests(unittest.TestCase):
    def test_java_properties_and_private_defaults(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'gradle.properties'
            path.write_text('screepsHost=http://localhost:21025/\n'
                            'screepsUser=Test\\ User\n'
                            'screepsPassword=pass\\=word\\#\\u0021\n'
                            'screepsToken=\n', encoding='utf-8')
            settings = Settings.load(path)
            self.assertEqual(settings.user, 'Test User')
            self.assertEqual(settings.password, 'pass=word#!')
            self.assertIsNone(settings.shard)
            self.assertEqual(settings.socket_url, 'ws://localhost:21025/socket/websocket')
            self.assertNotIn(settings.password, repr(settings))

    def test_official_shard_and_prefix(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'gradle.properties'
            path.write_text('screepsHost=https://screeps.com/ptr\nscreepsToken=fake-token\n')
            self.assertEqual(Settings.load(path).shard, 'shard0')
            self.assertEqual(Settings.load(path, 'shard2').socket_url,
                             'wss://screeps.com/ptr/socket/websocket')

    def test_no_credentials(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'gradle.properties'
            path.write_text('screepsHost=http://localhost:21025\n')
            with self.assertRaises(ConsoleError):
                Settings.load(path)


class ProtocolTests(unittest.TestCase):
    def test_console_frames(self):
        event = ['user:id/console', {'shard': 'shard1', 'messages': {
            'log': ['hello'], 'results': ['42']}, 'error': 'bad expression'}]
        frame = json.dumps(event)
        expected = [('log', 'hello'), ('result', '42'), ('error', 'bad expression')]
        self.assertEqual(console_lines(frame, 'shard1'), expected)
        compressed = 'gz:' + base64.b64encode(zlib.compress(frame.encode())).decode()
        self.assertEqual(console_lines(compressed), expected)
        self.assertEqual(console_lines(frame, 'shard2'), [])
        self.assertEqual(console_lines('time 123'), [])
        self.assertEqual(plain_text('<span style="color:red">a &lt; b</span>'), 'a < b')


class RunCommandTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which('node'), 'Node is required to verify the JavaScript wrapper')
    def test_wrapper_evaluates_values_errors_and_quoted_input(self):
        expressions = ['Game.time', '({answer:42})', 'undefined', 'null', 'false',
                       "'hello __MARKER__ \"world\"'", "throw new Error('failure')", 'Game.',
                       '(function(){var a={};a.self=a;return a})()']
        commands = [make_run_command(expression, 'test-marker:') for expression in expressions]
        program = "global.Game={time:42};process.stdout.write(JSON.stringify(JSON.parse(require('fs').readFileSync(0,'utf8')).map(code=>eval(code))));"
        result = subprocess.run([shutil.which('node'), '-e', program], input=json.dumps(commands),
                                text=True, capture_output=True, check=True)
        replies = [json.loads(item.removeprefix('test-marker:')) for item in json.loads(result.stdout)]
        self.assertEqual([item.get('result') for item in replies[:6]],
                         ['42', '{\n  "answer": 42\n}', 'undefined', 'null', 'false', 'hello __MARKER__ "world"'])
        self.assertFalse(replies[6]['ok'])
        self.assertIn('failure', replies[6]['error'])
        self.assertFalse(replies[7]['ok'])
        self.assertIn('SyntaxError', replies[7]['error'])
        self.assertIn('[Circular]', replies[8]['result'])
        self.assertLess(len(json.dumps(make_run_command('Game.time', '__screeps_cli_' + 'a' * 32 + '__:'))), 1024)

    def test_parser_accepts_connection_options_before_and_after_run(self):
        for arguments in (['--properties', 'custom.properties', '--shard', 'shard1', 'run', 'Game.time'],
                          ['run', 'Game.time', '--properties', 'custom.properties', '--shard', 'shard1']):
            with patch('screeps_cli.Settings.load', return_value=Settings('http://localhost', token='fake')) as load:
                with patch('screeps_cli.run_once', return_value=0) as once:
                    self.assertEqual(main(arguments), 0)
                    load.assert_called_once_with(Path('custom.properties'), 'shard1')
                    self.assertEqual(once.call_args.args[1], 'Game.time')

    def test_parser_rejects_invalid_timeout(self):
        for value in ('0', '-1', 'nan', 'inf', 'no'):
            with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as error:
                main(['run', 'Game.time', '--timeout', value])
            self.assertEqual(error.exception.code, 2)


class LegacyOutput:
    """An IDE output stream that cannot be reconfigured and rejects box drawing."""
    encoding = 'cp1251'

    def __init__(self):
        self.data = bytearray()

    def write(self, text):
        self.data.extend(text.encode(self.encoding))

    def flush(self):
        pass


class OutputTests(unittest.TestCase):
    def test_cp1251_reproduces_cpu_tree_failure(self):
        output = LegacyOutput()
        with self.assertRaises(UnicodeEncodeError):
            output.write('┗━ ActorSystem CPU: Привет')
        write_line('┗━ ActorSystem CPU: Привет 😀', stream=output)
        write_line('next tick', stream=output)
        text = output.data.decode('cp1251')
        self.assertIn(r'\u2517\u2501 ActorSystem CPU: Привет \U0001f600', text)
        self.assertIn('next tick', text)

    def test_real_stdio_is_utf8_before_prompt_captures_it(self):
        buffer = io.BytesIO()
        output = io.TextIOWrapper(buffer, encoding='cp1251', errors='strict')
        try:
            with patch('screeps_cli.sys.stdout', output), patch('screeps_cli.sys.stderr', io.StringIO()):
                configure_output()
                write_line('┗━ CPU: Привет 😀')
                write_line('bad surrogate: \ud800')
            text = buffer.getvalue().decode('utf-8')
            self.assertIn('┗━ CPU: Привет 😀', text)
            self.assertIn(r'\ud800', text)
        finally:
            output.close()


class TerminalTests(unittest.IsolatedAsyncioTestCase):
    async def test_log_flood_and_resize_preserve_input_and_cursor_row(self):
        from prompt_toolkit.input import create_pipe_input
        from prompt_toolkit.output import DummyOutput
        from prompt_toolkit.data_structures import Size

        class Output(DummyOutput):
            rows = 12

            def get_size(self):
                return Size(rows=self.rows, columns=60)

        output = Output()
        with create_pipe_input() as pipe:
            ui = TerminalConsole(lambda expression: None, lambda: True,
                                 lambda kind, line: ui.post(str(line)), input=pipe, output=output)
            task = asyncio.create_task(ui.app.run_async(pre_run=ui.start, handle_sigint=False))
            try:
                await self.wait_for(lambda: ui.app.is_running)
                pipe.send_text('Game.creeps')
                await self.wait_for(lambda: ui.input.text == 'Game.creeps')
                await self.wait_for(lambda: ui.app.renderer.last_rendered_screen is not None)
                await asyncio.sleep(0.1)
                before = ui.app.renderer.last_rendered_screen.cursor_positions[ui.input.window]
                self.assertEqual(before.y, output.rows - 1)
                position = ui.input.buffer.cursor_position
                for n in range(2500):
                    ui.post(f'┗━ log {n}: ' + 'a long log line ' * 10)
                await self.wait_for(lambda: ui.lines and ui.lines[-1].startswith('┗━ log 2499:'))
                await asyncio.sleep(0.1)
                after = ui.app.renderer.last_rendered_screen.cursor_positions[ui.input.window]
                self.assertEqual((after.x, after.y), (before.x, before.y))
                self.assertEqual(ui.input.text, 'Game.creeps')
                self.assertEqual(ui.input.buffer.cursor_position, position)
                self.assertEqual(len(ui.lines), ui.MAX_LOG_LINES)
                self.assertIs(ui.app.layout.current_control, ui.input.control)
                output.rows = 20
                ui.app.invalidate()
                await self.wait_for(
                    lambda: ui.app.renderer.last_rendered_screen.cursor_positions[ui.input.window].y == 19)
                pipe.send_text('.Spawn1')
                await self.wait_for(lambda: ui.input.text == 'Game.creeps.Spawn1')
                pipe.send_bytes(b'\x03')
                await self.wait_for(lambda: ui.input.text == '')
                pipe.send_bytes(b'\x04')
                await asyncio.wait_for(task, 2)
            finally:
                if not task.done():
                    ui.app.exit()
                    await task
                ui.stop()

    async def test_command_request_does_not_block_typing_or_log_rendering(self):
        from prompt_toolkit.input import create_pipe_input
        from prompt_toolkit.output import DummyOutput

        client = ScreepsClient(Settings('http://localhost:21025', token='fake-token'))
        started = threading.Event()
        release = threading.Event()
        commands = []

        def execute(expression):
            with client.lock:
                commands.append(expression)
                started.set()
                release.wait(3)

        with create_pipe_input() as pipe:
            ui = TerminalConsole(execute, lambda: True,
                                 lambda kind, line: ui.post(client.redact(line)), input=pipe, output=DummyOutput())
            task = asyncio.create_task(ui.app.run_async(pre_run=ui.start, handle_sigint=False))
            try:
                await self.wait_for(lambda: ui.app.is_running)
                pipe.send_text('Game.time\r')
                await self.wait_for(started.is_set)
                pipe.send_text('Memory.settings')
                ui.post('log while request is waiting')
                await self.wait_for(lambda: ui.input.text == 'Memory.settings' and
                                            'log while request is waiting' in ui.logs.text)
                self.assertFalse(release.is_set())
                self.assertEqual(commands, ['Game.time'])
                # The next submit also redacts/echoes without acquiring the HTTP request lock.
                pipe.send_text('\r')
                await self.wait_for(lambda: ui.input.text == '')
                await self.wait_for(lambda: 'Memory.settings' in ui.logs.text)
                release.set()
                await self.wait_for(lambda: commands == ['Game.time', 'Memory.settings'])
                pipe.send_bytes(b'\x1b[A')
                await self.wait_for(lambda: ui.input.text == 'Memory.settings')
                pipe.send_bytes(b'\x04')
                await asyncio.wait_for(task, 2)
            finally:
                release.set()
                if not task.done():
                    ui.app.exit()
                    await task
                ui.stop()
                client.http.close()

    async def wait_for(self, predicate):
        deadline = time.monotonic() + 2
        while not predicate() and time.monotonic() < deadline:
            await asyncio.sleep(0.02)
        self.assertTrue(predicate())


class MockHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, *args):
        pass

    def reply(self, data, status=200, token=None):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        if token:
            self.send_header('X-Token', token)
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        data = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        if self.path == '/api/auth/signin':
            if data != {'email': 'test-user', 'password': 'test-password'}:
                self.reply({'error': 'unauthorized'}, 401)
                return
            self.server.logins += 1
            self.server.token = f'session-{self.server.logins}'
            self.reply({'ok': 1, 'token': self.server.token})
        elif self.path == '/api/user/console':
            if self.server.reject_next:
                self.server.reject_next = False
                self.reply({'error': 'unauthorized'}, 401)
                return
            if not self.authorized():
                self.reply({'error': 'unauthorized'}, 401)
                return
            self.server.commands.append(data)
            self.reply({'ok': 1})
            if self.server.stream:
                results = self.server.command_results(data['expression']) if self.server.command_results else ['42']
                self.server.stream.send_frame(json.dumps(['user:test-id/console', {
                    'messages': {'log': ['unrelated game log'], 'results': results}}]))
        else:
            self.reply({'error': 'unknown path'}, 404)

    def authorized(self):
        return (self.headers.get('X-Token') == self.server.token
                and self.headers.get('X-Username') == self.server.token)

    def do_GET(self):
        if self.path == '/api/auth/me':
            if self.authorized():
                self.reply({'ok': 1, '_id': 'test-id'}, token=self.server.token)
            else:
                self.reply({'error': 'unauthorized'}, 401)
        elif self.path == '/socket/websocket':
            self.websocket()
        else:
            self.reply({'error': 'unknown path'}, 404)

    def read_frame(self):
        head = self.rfile.read(2)
        if len(head) != 2:
            return None
        opcode, size = head[0] & 15, head[1] & 127
        if size == 126:
            size = struct.unpack('!H', self.rfile.read(2))[0]
        elif size == 127:
            size = struct.unpack('!Q', self.rfile.read(8))[0]
        mask = self.rfile.read(4) if head[1] & 128 else b''
        payload = self.rfile.read(size)
        if opcode == 8:
            return None
        if mask:
            payload = bytes(value ^ mask[i % 4] for i, value in enumerate(payload))
        return payload.decode()

    def send_frame(self, text):
        data = text.encode()
        header = bytes([129, len(data)]) if len(data) < 126 else bytes([129, 126]) + struct.pack('!H', len(data))
        with self.server.frame_lock:
            self.connection.sendall(header + data)

    def websocket(self):
        accept = base64.b64encode(hashlib.sha1((self.headers['Sec-WebSocket-Key'] +
                                                '258EAFA5-E914-47DA-95CA-C5AB0DC85B11').encode()).digest()).decode()
        self.send_response(101)
        self.send_header('Upgrade', 'websocket')
        self.send_header('Connection', 'Upgrade')
        self.send_header('Sec-WebSocket-Accept', accept)
        self.end_headers()
        self.connection.settimeout(5)
        self.close_connection = True
        try:
            self.send_frame('protocol 14')
            self.send_frame('time 123')
            if self.read_frame() != 'auth ' + self.server.token:
                self.send_frame('auth failed')
                return
            self.server.token = 'socket-' + self.server.token
            self.send_frame('auth ok ' + self.server.token)
            if self.read_frame() != 'subscribe user:test-id/console':
                return
            self.server.stream = self
            self.send_frame(json.dumps(['user:test-id/console', {'messages': {'log': self.server.boot_messages}}]))
            while self.read_frame() is not None:
                pass
        except (OSError, ValueError):
            pass


class ClientIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.server = ThreadingHTTPServer(('127.0.0.1', 0), MockHandler)
        self.server.daemon_threads = True
        self.server.logins = 0
        self.server.boot_messages = ['boot log']
        self.server.token = 'persistent-token'
        self.server.commands = []
        self.server.command_results = None
        self.server.reject_next = False
        self.server.stream = None
        self.server.frame_lock = threading.Lock()
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.settings = Settings(f'http://127.0.0.1:{self.server.server_port}', 'test-user', 'test-password')
        self.client = ScreepsClient(self.settings)
        self.stream = None

    def tearDown(self):
        if self.stream:
            self.stream.stop()
        self.client.http.close()
        self.server.shutdown()
        self.server.server_close()

    def test_login_rotation_console_and_reconnect(self):
        events = []
        self.stream = ConsoleStream(self.client, lambda kind, line: events.append((kind, line)))
        self.stream.start()
        self.assertTrue(self.stream.ready.wait(3))
        self.wait_for(lambda: ('log', 'boot log') in events)
        self.assertEqual(self.client.token, 'socket-session-1')
        self.client.execute('Game.time')
        self.wait_for(lambda: ('result', '42') in events)
        self.assertEqual(self.server.commands, [{'expression': 'Game.time', 'shard': None}])
        self.server.stream.connection.shutdown(socket.SHUT_RDWR)
        self.wait_for(lambda: sum(line == 'Console stream connected' for _, line in events) == 2)

    def test_unicode_logs_do_not_disconnect_legacy_output(self):
        self.server.boot_messages = ['Root CPU: total=2.08', '┗━ ActorSystem CPU: Привет 😀', 'next tick']
        output = LegacyOutput()
        events = []
        delivered = threading.Event()

        def emit(kind, line):
            write_line(line, stream=output)
            events.append((kind, line))
            if line == 'next tick':
                delivered.set()

        self.stream = ConsoleStream(self.client, emit)
        self.stream.start()
        self.assertTrue(delivered.wait(3))
        self.client.execute('Game.time')
        self.wait_for(lambda: ('result', '42') in events)
        self.assertTrue(self.stream.ready.is_set())
        self.assertEqual(self.server.logins, 1)
        self.assertFalse(any('disconnected' in line for _, line in events))
        self.assertIn('next tick', output.data.decode('cp1251'))

    def test_check_mode_without_windows_console(self):
        output = io.StringIO()
        with redirect_stdout(output):
            self.assertEqual(run(self.settings, check=True), 0)
        self.assertEqual(self.server.commands, [])
        self.assertIn('Connection check passed', output.getvalue())

    def test_piped_command_loop(self):
        output = io.StringIO()
        with redirect_stdout(output), patch('builtins.input', side_effect=['Game.time', '/quit']):
            self.assertEqual(run(self.settings), 0)
        self.assertEqual(len(self.server.commands), 1)
        self.assertEqual(self.server.commands[0]['expression'], 'Game.time')
        self.assertNotIn('test-password', output.getvalue())

    def one_shot_reply(self, expression, payload):
        import re
        marker = re.search(r'__screeps_cli_[a-f0-9]{32}__:', expression).group()
        return ['unrelated command result', marker + json.dumps(payload)]

    def test_run_once_prints_only_correlated_result(self):
        self.server.command_results = lambda expr: self.one_shot_reply(expr, {'ok': True, 'result': '42'})
        output, errors = io.StringIO(), io.StringIO()
        with redirect_stdout(output), redirect_stderr(errors):
            self.assertEqual(run_once(self.settings, 'Game.time', timeout=1), 0)
        self.assertEqual(output.getvalue(), '42\n')
        self.assertEqual(errors.getvalue(), '')
        self.assertEqual(len(self.server.commands), 1)

    def test_run_once_javascript_error_is_stderr_and_nonzero(self):
        self.server.command_results = lambda expr: self.one_shot_reply(expr, {'ok': False,
                                                                              'error': 'ReferenceError: missing'})
        output, errors = io.StringIO(), io.StringIO()
        with redirect_stdout(output), redirect_stderr(errors):
            self.assertEqual(run_once(self.settings, 'missing', timeout=1), 1)
        self.assertEqual(output.getvalue(), '')
        self.assertEqual(errors.getvalue(), 'ReferenceError: missing\n')
        self.assertEqual(len(self.server.commands), 1)

    def test_run_once_timeout_ignores_other_results_and_does_not_resend(self):
        self.server.command_results = lambda expr: ['unrelated result']
        output, errors = io.StringIO(), io.StringIO()
        with redirect_stdout(output), redirect_stderr(errors):
            self.assertEqual(run_once(self.settings, 'Game.time', timeout=0.1), 124)
        self.assertEqual(output.getvalue(), '')
        self.assertIn('timed out waiting', errors.getvalue())
        self.assertEqual(len(self.server.commands), 1)

    def test_expired_session_is_renewed_before_command(self):
        self.client.identity()
        self.client.token_updated -= 61
        self.client.execute('Game.time')
        self.assertEqual(self.server.logins, 2)
        self.assertEqual(len(self.server.commands), 1)

    def test_unauthorized_retries_once_with_new_session(self):
        self.client.identity()
        self.server.reject_next = True
        self.client.execute('Game.time')
        self.assertEqual(self.server.logins, 2)
        self.assertEqual(len(self.server.commands), 1)

    def test_persistent_token_without_password(self):
        self.client.http.close()
        self.client = ScreepsClient(Settings(self.settings.host, token='persistent-token'))
        self.assertEqual(self.client.identity(), 'test-id')
        self.assertEqual(self.server.logins, 0)

    def test_timed_out_command_is_not_retried(self):
        self.client.identity()
        with patch.object(self.client.http, 'request', side_effect=requests.Timeout) as request:
            with self.assertRaisesRegex(ConsoleError, 'not retried'):
                self.client.execute('Game.time')
            self.assertEqual(request.call_count, 1)

    def test_secrets_are_redacted(self):
        self.client.identity()
        self.assertEqual(self.client.redact('test-password session-1'), '[REDACTED] [REDACTED]')

    def wait_for(self, predicate):
        deadline = time.monotonic() + 5
        while not predicate() and time.monotonic() < deadline:
            time.sleep(0.02)
        self.assertTrue(predicate())


if __name__ == '__main__':
    unittest.main()
