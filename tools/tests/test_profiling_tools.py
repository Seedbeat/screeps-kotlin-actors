"""Offline regression checks for console capture and CPU log analysis."""
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from analyze_logs import main as analyze_main, markdown_report, parse_logs, summarize
from capture_logs import capture_logs, positive_seconds
import argparse


def tick(time, cpu, profile="", system_time=None):
    lifecycle = f"[INFO][SystemActor]: System tick: {system_time}\n" if system_time is not None else ""
    return (f"[INFO][Root]: === Tick: {time} ===\n" + lifecycle + profile
            + f"[INFO][Root]: === CPU: {cpu} ===\n")


class AnalysisTests(unittest.TestCase):
    def test_nested_paths_statistics_and_incomplete_tick(self):
        profile = ("[INFO][Root]: ActorSystem CPU: total=1.5 self=0.2\n"
                   "[INFO][Root]:  ┗━ runTickRound CPU: total=1.3 self=0.1\n"
                   "[INFO][Root]: room CPU: total=0.4 self=0.0\n"
                   "[INFO][Root]:  ┗━ Lifecycle CPU: total=0.4 self=0.0\n"
                   "[INFO][Root]:     ┗━ Tick CPU: total=0.4 self=0.05\n"
                   "[INFO][Root]:        ┗━ processIntents CPU: total=0.35 self=0.05\n"
                   "[INFO][Root]:           ┗━ PlanWorkforce CPU: total=0.3 self=0.3\n")
        text = ("[12:00:00] error | disconnected\n" + tick(10, 2, profile, 10)
                + "[INFO][Root]: === Tick: 11 ===\n"
                + tick(12, 4, profile.replace("total=1.5", "total=3.5"), 11))
        ticks, issues = parse_logs(text.splitlines())
        result = summarize(ticks, issues)
        self.assertEqual(result["ticks"], 2)
        self.assertEqual(result["cpu"]["mean"], 3)
        self.assertEqual(result["cpu"]["p95"], 4)
        self.assertEqual(result["outside_actor_system"]["mean"], 0.5)
        self.assertEqual(result["missing_tick_ranges"], [[11, 11]])
        self.assertEqual(result["incomplete_ticks"], [11])
        self.assertEqual(result["system_tick_lag"]["max"], 1)
        self.assertEqual(len(result["errors_or_stops"]), 1)
        self.assertEqual(result["nodes"]["room/Lifecycle/Tick/processIntents/PlanWorkforce"]["mean_per_tick"], 0.3)

    def test_disabled_profiling_still_produces_cpu_report(self):
        ticks, issues = parse_logs((tick(1, 2) + tick(2, 3)).splitlines())
        result = summarize(ticks, issues)
        self.assertEqual(result["cpu"]["mean"], 2.5)
        self.assertIsNone(result["outside_actor_system"])
        self.assertIsNone(result["system_tick_lag"])
        self.assertEqual(result["nodes"], {})
        self.assertIn("No profiling tree was observed", markdown_report(result))

    def test_filtered_tree_missing_root_and_periodic_frequency(self):
        profile = ("[INFO][Root]: room CPU: total=4 self=0\n"
                   "[INFO][Root]:  ┗━ SyncPlanningCache CPU: total=4 self=4\n")
        ticks, issues = parse_logs((tick(1, 6, profile) + tick(2, 2)).splitlines())
        result = summarize(ticks, issues)
        self.assertIsNone(result["outside_actor_system"])
        node = result["nodes"]["room/SyncPlanningCache"]
        self.assertEqual(node["ticks_present"], 1)
        self.assertEqual(node["total_when_present"]["mean"], 4)
        self.assertEqual(node["mean_per_tick"], 2)

    def test_orphan_tree_nodes_and_truncated_tail_are_reported(self):
        text = tick(1, 2, "[INFO][Root]:     ┗━ Orphan CPU: total=1 self=1\n")
        ticks, issues = parse_logs((text + "[INFO][Root]: === Tick: 2 ===\n").splitlines())
        result = summarize(ticks, issues)
        self.assertEqual(result["malformed_profile_lines"], 1)
        self.assertEqual(result["incomplete_ticks"], [2])
        self.assertEqual(result["nodes"], {})

    def test_tick_restart_and_duplicates_do_not_create_large_gap(self):
        ticks, issues = parse_logs((tick(1000000, 1) + tick(1000000, 1) + tick(1, 1)).splitlines())
        result = summarize(ticks, issues)
        self.assertEqual(result["duplicate_ticks"], [1000000])
        self.assertEqual(result["out_of_order_ticks"], [1])
        self.assertEqual(result["missing_tick_count"], 0)

    def test_cli_writes_artifacts_and_rejects_empty_sample_cleanly(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            log = root / "sample.log"
            output = root / "report"
            log.write_text(tick(1, 2), encoding="utf-8")
            with redirect_stdout(io.StringIO()):
                self.assertEqual(analyze_main([str(log), "--output-dir", str(output)]), 0)
            self.assertEqual(json.loads((output / "summary.json").read_text())["ticks"], 1)
            self.assertTrue((output / "ticks.json").is_file())
            self.assertTrue((output / "report.md").is_file())
            log.write_text("No events\n", encoding="utf-8")
            error = io.StringIO()
            with redirect_stderr(error):
                self.assertEqual(analyze_main([str(log)]), 1)
            self.assertIn("No complete ticks", error.getvalue())


class CaptureTests(unittest.TestCase):
    def test_capture_duration_is_bounded_and_preserves_unicode(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cli = root / "fake_cli.py"
            cli.write_text("import time\nprint('┗━ CPU', flush=True)\ntime.sleep(30)\n", encoding="utf-8")
            output = root / "nested" / "sample.log"
            self.assertEqual(capture_logs(2, output, cli=cli), 0)
            self.assertIn("┗━ CPU", output.read_text(encoding="utf-8"))

    def test_child_failure_and_connection_arguments_are_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cli = root / "fake_cli.py"
            cli.write_text("import json, sys\nprint(json.dumps(sys.argv[1:]))\nsys.exit(7)\n")
            output = root / "sample.log"
            properties = root / "fake.properties"
            self.assertEqual(capture_logs(5, output, cli=cli, properties=properties, shard="shard2"), 7)
            self.assertEqual(json.loads(output.read_text()),
                             ["--logs-only", "--properties", str(properties.resolve()), "--shard", "shard2"])

    def test_invalid_durations(self):
        for value in ("0", "-1", "nan", "inf", "text"):
            with self.subTest(value=value), self.assertRaises(argparse.ArgumentTypeError):
                positive_seconds(value)


if __name__ == "__main__":
    unittest.main()
