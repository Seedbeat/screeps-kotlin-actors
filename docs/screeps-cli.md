# Screeps Game Console

A terminal console built on [python-screeps](https://github.com/screepers/python-screeps) and its
[endpoint reference](https://github.com/screepers/python-screeps/blob/master/docs/Endpoints.md).
It follows live game logs over WebSocket and sends JavaScript expressions through `POST /api/user/console`.
This is the game console (`Game`, `Memory`), not the administrative server CLI (`storage`, `system`, port 21026).

## Install And Run

Use Python 3.10 or newer. From the project root on Windows:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r tools\requirements.txt
.\screeps-cli.bat
```

The environment has already been created if these setup steps were completed in this workspace.
The launcher works from other directories too; the default properties file is always the project's `gradle.properties`.
You can also run Python directly:

```powershell
.\.venv\Scripts\python.exe tools\screeps_cli.py
```

On Linux/macOS:

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r tools/requirements.txt
.venv/bin/python tools/screeps_cli.py
```

Additional modes:

```powershell
.\screeps-cli.bat --check
.\screeps-cli.bat --logs-only
.\screeps-cli.bat --properties C:\path\to\gradle.properties --shard shard1
```

`--check` verifies HTTP authentication and the WebSocket subscription, then exits without sending game commands.
`--logs-only` follows the stream without a command prompt, including when output is redirected to a file.

## Run One Command

Execute a JavaScript expression, print its result, and exit:

```powershell
.\screeps-cli run "Game.time"
.\screeps-cli run "Object.keys(Game.creeps)" --timeout 60
$tick = .\screeps-cli run "Game.time"
```

PowerShell resolves `screeps-cli.bat` when the extension is omitted. Python can run the same command on every platform:

```sh
.venv/bin/python tools/screeps_cli.py run "Game.time"
```

Connection settings still come from `gradle.properties`. `--properties` and `--shard` work before or after `run`.
Only the command result is written to stdout, without timestamps, connection status, or game logs. Strings are printed
as text, objects/arrays as indented JSON, and `undefined` as `undefined`. Errors go to stderr. Exit codes are 0 for
success, 1 for JavaScript/API/connection errors, 2 for invalid CLI arguments, 124 for a result timeout, and 130 for
Ctrl+C.

The client subscribes before submitting the command, and wraps the expression in a JavaScript evaluation with a unique
result marker. This matches the reply even when other consoles execute commands at the same time. The wrapper catches
JavaScript errors (including syntax errors) and does not persist its own state in Memory. Circular object references are
rendered as `[Circular]`. The wrapper counts towards the server's console-expression size limit.

`--timeout` is the result wait after the HTTP submission completes (30 seconds by default); HTTP and socket connection
timeouts apply separately. A paused server must resume ticking to evaluate the command. A timeout or dropped result
does not resend the command, because its side effects may already have happened. `run` cannot be combined with
`--check` or `--logs-only`.

## Connection Settings

The CLI reads the local file itself; it does not invoke Gradle or merge settings from the global Gradle configuration,
`-P` arguments, or environment variables. The properties file uses Java properties escaping, comments, and continuation
rules and is read as UTF-8 (an optional BOM is accepted). Do not add shell quotes around property values.

For a private server with password login:

```properties
screepsHost=http://private-server:21025
screepsUser=YOUR_API_USERNAME
screepsPassword=YOUR_API_PASSWORD
```

These credentials are the ones configured by `screepsmod-auth`. The CLI signs in via `/api/auth/signin`, obtains the
user
id via `/api/auth/me`, and subscribes to `user:<id>/console` at `/socket/websocket`. It sends both `X-Token` and
`X-Username` on authenticated API requests. A private server without password-auth support requires a valid token
obtained by another supported login method; the CLI does not generate Steam tickets.

Optional properties:

| Property                | Behavior                                                                                      |
|-------------------------|-----------------------------------------------------------------------------------------------|
| `screepsHost`           | Defaults to `https://screeps.com`; includes scheme, port, and optional prefix such as `/ptr`. |
| `screepsToken`          | Used first when nonblank. Omit it to sign in with username/password.                          |
| `screepsShard`          | Defaults to `shard0` on `screeps.com`; omitted for private servers. `--shard` overrides it.   |
| `screepsServerPassword` | Optional server-wide password sent as `X-Server-Password`; separate from the user's password. |

Session tokens are kept in memory and updated from HTTP and socket responses. Password sessions are renewed before
commands after 40 seconds without token refresh. An HTTP 401 triggers one fresh sign-in and one retry if credentials
are available, including when the initially configured token was rejected. Timeouts and transport failures do not
retry commands because the server might already have accepted them. HTTP connection/read timeouts are 5/15 seconds;
socket authentication is bounded, and stream reconnection backs off to at most 15 seconds between attempts.

The known persistent-token rejection on some private-server routes is not fixed by this client. Configuring username
and password allows it to obtain a session token if a command receives HTTP 401. It never changes `gradle.properties`
or writes tokens or command history to disk. Credentials and observed tokens are redacted from displayed output.

## Use The Console

The launcher enables Python UTF-8 mode, and the CLI configures stdout/stderr as UTF-8 before the prompt captures them.
This preserves CPU tree characters such as `┗━` in Windows terminals and redirected output. Embedded IDE streams that
cannot be reconfigured escape unsupported characters instead of failing. Output encoding errors no longer enter the
socket reconnection handler. Invalid JSON/compressed console frames are skipped separately, and successful socket
authentication resets the reconnection backoff.

Interactive mode uses a full-screen terminal layout: logs occupy the upper pane and the single command row stays
fixed at the bottom. Incoming logs update only the log pane, preserving the typed command, cursor position, focus, and
history. Long commands scroll horizontally within their row. The log pane keeps the latest 2,000 lines and follows new
events; updates are batched to keep typing responsive. HTTP commands run in a separate worker so waiting for a response
does not freeze input or log rendering.

Run `screeps-cli.bat` from Windows Terminal, PowerShell, or an IDE's Terminal tab. An IDE Run/Output panel with
redirected
stdin/stdout does not provide terminal cursor controls; the CLI reports plain input mode there. `--logs-only` remains
suitable for redirected output and log files.

Enter sends one JavaScript expression; results and errors appear in the log pane. For example:

```javascript
Game.time
Object.keys(Game.creeps)
Memory.settings
```

Local commands:

- `/help`: show controls.
- `/clear` or Ctrl+L: clear the log pane while keeping the input row fixed.
- `/quit` or `/exit`: disconnect and exit.
- Ctrl+C: clear the current input; in logs-only mode, exit.
- Ctrl+D: exit the interactive console. `/quit` works on Windows terminals too.
- Up/Down: recall commands from this session; Right accepts a history suggestion.

Only new events from the subscription are available; this API does not replay historical console logs. Events during
a disconnection may be missed. Results are produced when the game processes the command; a paused server may not
return a result until ticking resumes. The connection status confirms authentication and that the subscription was
sent; the protocol does not acknowledge subscriptions separately.

## Capture And Analyze CPU Logs

Use these reusable tools for performance investigations:

```powershell
.\.venv\Scripts\python.exe tools\capture_logs.py --seconds 45 --output build\performance\baseline.log
.\.venv\Scripts\python.exe tools\analyze_logs.py build\performance\baseline.log --output-dir build\performance\baseline-analysis
```

`capture_logs.py` runs the existing CLI in `--logs-only` mode, saves UTF-8 output, and terminates its child process at
the deadline or on Ctrl+C. The duration includes connection startup. Early CLI failures retain their exit code and
diagnostics in the log. It accepts `--properties` and `--shard` with the same meaning as the console. Without
`--output`,
it creates a timestamped log under the project's `build/performance`, even when invoked from another directory.
Explicit paths are relative to the current working directory. Capture sends no game commands and does not change
profiling settings or deploy a bundle.

`analyze_logs.py` works offline and writes `report.md`, `summary.json`, and `ticks.json`. Without `--output-dir`, it
creates `<log-stem>-analysis` beside the log. It computes mean, median, nearest-rank p95, min/max, observed profiling
paths with both total and exclusive CPU, periodic sync groups, and visible SystemActor tick lag. It also records
incomplete ticks, missing tick ranges, duplicates, resets, errors, and connection events. Incomplete ticks are excluded
from CPU statistics. An empty sample fails with an explanatory message rather than a traceback.

The analyzer also accepts logs captured by redirecting `screeps-cli --logs-only`. With profiling disabled or tree output
filtered, it still analyzes Root CPU summary lines; unavailable tree metrics remain absent or null. To obtain detailed
trees, the deployed bot must have profiling enabled; these tools do not enable it automatically.

Interpret measurements carefully: parent and child paths overlap, and ActorSystem includes actor execution, so their
totals must not be added. Root CPU minus ActorSystem includes other work outside the scheduler as well as report output;
it is not a direct measurement of printing overhead. The Root summary precedes final log output and engine Memory
serialization. Missing paths or suppressed logs can understate costs, and no visible scheduler stop does not prove
empty queues. Keep generated samples and reports under the ignored `build` directory.

## Verify

The CLI is independent of the Kotlin runtime and does not change actor behavior, persistence, or the deployed bundle.
Tests use a local HTTP/WebSocket server with fake credentials, so they do not access your Screeps server:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tools/tests -v
```

They cover Java properties parsing, auth/session rotation, expiry renewal, command delivery, HTTP 401 recovery,
non-retry on timeout, stream reconnection, compressed frames, shard filtering, secret redaction, and Unicode CPU
logs on strict Windows-1251 output streams without disconnecting. Terminal tests render the actual layout and verify
that 2,500 long log lines and window resizing preserve the input text/cursor, and that slow requests do not block
typing.
One-command tests cover correlated results, clean stdout/stderr, JavaScript errors, timeout without resubmission,
argument parsing, and the JavaScript wrapper itself (the wrapper test uses Node when available).
Profiling tool tests cover bounded child-process capture, connection arguments and failure codes, Unicode, nested CPU
paths, disabled/filtered profiling, incomplete samples, tick gaps and restarts, and offline report generation.
