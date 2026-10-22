#!/usr/bin/env python3
"""Capture a bounded Screeps console stream using the existing screeps-cli client."""
from __future__ import annotations

import argparse
import math
import subprocess
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CLI = Path(__file__).with_name("screeps_cli.py")


def positive_seconds(value: str) -> float:
    try:
        seconds = float(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("seconds must be a positive finite number") from exc
    if not math.isfinite(seconds) or seconds <= 0:
        raise argparse.ArgumentTypeError("seconds must be a positive finite number")
    return seconds


def capture_logs(seconds: float, output: Path, *, properties: Path | None = None,
                 shard: str | None = None, cli: Path = CLI) -> int:
    command = [sys.executable, "-X", "utf8", str(cli), "--logs-only"]
    if properties is not None:
        command.extend(["--properties", str(properties.resolve())])
    if shard is not None:
        command.extend(["--shard", shard])
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as target:
        child = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL,
                                 stdout=target, stderr=subprocess.STDOUT)
        try:
            try:
                return child.wait(timeout=seconds)
            except subprocess.TimeoutExpired:
                # Reaching the requested capture duration is a normal completion.
                return 0
        finally:
            if child.poll() is None:
                child.terminate()
                try:
                    child.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    child.kill()
                    child.wait(timeout=5)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seconds", type=positive_seconds, default=45,
                        help="Capture duration including connection startup (default: 45)")
    parser.add_argument("--output", type=Path,
                        help="UTF-8 log path; defaults to a timestamped file in build/performance")
    parser.add_argument("--properties", type=Path, help="Forward a properties file to screeps-cli")
    parser.add_argument("--shard", help="Forward a shard selection to screeps-cli")
    args = parser.parse_args(argv)
    output = args.output or ROOT / "build" / "performance" / datetime.now().strftime("capture-%Y%m%d-%H%M%S-%f.log")
    try:
        result = capture_logs(args.seconds, output, properties=args.properties, shard=args.shard)
    except KeyboardInterrupt:
        print(f"Capture interrupted; partial log: {output.resolve()}", file=sys.stderr)
        return 130
    except OSError as exc:
        print(f"Cannot capture logs: {exc}", file=sys.stderr)
        return 1
    print(f"Saved log: {output.resolve()} ({output.stat().st_size} bytes)")
    if result:
        print(f"screeps-cli exited with code {result}; see the captured log for details", file=sys.stderr)
    return result if result >= 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
