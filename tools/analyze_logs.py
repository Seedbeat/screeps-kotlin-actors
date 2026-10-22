#!/usr/bin/env python3
"""Analyze saved Root CPU summaries and profiling trees without connecting to Screeps."""
from __future__ import annotations

import argparse
import json
import math
import re
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

TICK = re.compile(r"=+ Tick: (\d+) =+")
CPU = re.compile(r"=+ CPU: (\d+(?:\.\d+)?) =+")
NODE = re.compile(r"Root\]: (.*?)\s+CPU: total=(\d+(?:\.\d+)?) self=(\d+(?:\.\d+)?)")
SYSTEM_TICK = re.compile(r"System tick: (\d+)")


def parse_logs(lines):
    ticks = []
    issues = {"incomplete_ticks": [], "malformed_profile_lines": 0,
              "duplicate_node_lines": 0, "errors_or_stops": [], "stream_events": []}
    current = None
    stack = []
    for raw in lines:
        line = raw.rstrip("\r\n")
        if "[ERROR]" in line or "error |" in line or "Scheduler stop" in line or "RESET RESET" in line:
            issues["errors_or_stops"].append(line)
        if "status |" in line:
            issues["stream_events"].append(line)
        start = TICK.search(line)
        if start:
            if current is not None:
                issues["incomplete_ticks"].append(current["tick"])
            current = {"tick": int(start[1]), "nodes": {}, "lines": 1, "lifecycle": []}
            stack = []
            continue
        if current is None:
            continue
        current["lines"] += 1
        lifecycle = SYSTEM_TICK.search(line)
        if lifecycle:
            current["lifecycle"].append(int(lifecycle[1]))
        end = CPU.search(line)
        if end:
            current["cpu"] = float(end[1])
            ticks.append(current)
            current = None
            continue
        node = NODE.search(line)
        if not node:
            if "CPU: total=" in line:
                issues["malformed_profile_lines"] += 1
                stack = []
            continue
        label = node[1]
        if "┗━" in label:
            depth = (label.index("┗━") + 2) // 3
            label = label.split("┗━", 1)[1].strip()
        else:
            depth = 0
            label = label.strip()
        if depth > len(stack) or not label:
            issues["malformed_profile_lines"] += 1
            stack = []
            continue
        stack = stack[:depth] + [label]
        path = "/".join(stack)
        if path in current["nodes"]:
            issues["duplicate_node_lines"] += 1
        current["nodes"][path] = {"total": float(node[2]), "self": float(node[3])}
    if current is not None:
        issues["incomplete_ticks"].append(current["tick"])
    return ticks, issues


def stats(values):
    if not values:
        return None
    ordered = sorted(values)
    return {"mean": round(statistics.mean(values), 4), "median": round(statistics.median(values), 4),
            "p95": ordered[math.ceil(0.95 * len(ordered)) - 1], "min": ordered[0], "max": ordered[-1]}


def summarize(ticks, issues):
    if not ticks:
        raise ValueError("No complete ticks with a Root CPU summary found in the log")
    paths = sorted({path for tick in ticks for path in tick["nodes"]})
    nodes = {}
    for path in paths:
        observed = [tick["nodes"][path] for tick in ticks if path in tick["nodes"]]
        nodes[path] = {
            "ticks_present": len(observed),
            "total_when_present": stats([node["total"] for node in observed]),
            "self_when_present": stats([node["self"] for node in observed]),
            "mean_per_tick": round(sum(node["total"] for node in observed) / len(ticks), 4),
        }
    outside = [tick["cpu"] - tick["nodes"]["ActorSystem"]["total"]
               for tick in ticks if "ActorSystem" in tick["nodes"]]
    groups = defaultdict(list)
    for tick in ticks:
        syncs = sorted({path.split("/")[-1] for path in tick["nodes"]
                        if path.split("/")[-1].startswith("Sync")})
        groups[",".join(syncs) or "no periodic sync observed"].append(tick["cpu"])
    counts = Counter(tick["tick"] for tick in ticks)
    missing_ranges = [[left["tick"] + 1, right["tick"] - 1] for left, right in zip(ticks, ticks[1:])
                      if right["tick"] > left["tick"] + 1]
    return {
        "ticks": len(ticks), "first_tick": ticks[0]["tick"], "last_tick": ticks[-1]["tick"],
        "missing_tick_ranges": missing_ranges,
        "missing_tick_count": sum(end - start + 1 for start, end in missing_ranges),
        "duplicate_ticks": sorted(tick for tick, count in counts.items() if count > 1),
        "out_of_order_ticks": [right["tick"] for left, right in zip(ticks, ticks[1:])
                               if right["tick"] < left["tick"]],
        "cpu": stats([tick["cpu"] for tick in ticks]),
        "outside_actor_system": stats(outside), "ticks_with_actor_system": len(outside),
        "log_lines_per_tick": stats([tick["lines"] for tick in ticks]),
        "system_tick_lag": stats([tick["tick"] - time for tick in ticks for time in tick["lifecycle"]]),
        "periodic_groups": {key: {"count": len(values), **stats(values)} for key, values in groups.items()},
        "nodes": nodes, **issues,
    }


def markdown_report(summary):
    cpu = summary["cpu"]
    lines = ["# Screeps CPU report", "",
             f"Complete ticks: **{summary['ticks']}**, {summary['first_tick']}–{summary['last_tick']}.", "",
             "| Root CPU | Mean | Median | p95 | Min | Max |", "|---|---:|---:|---:|---:|---:|",
             "| CPU | " + " | ".join(f"{cpu[key]:.3f}" for key in ("mean", "median", "p95", "min", "max")) + " |", "",
             f"Missing ticks between complete samples: {summary['missing_tick_count']}.",
             f"Incomplete ticks excluded: {len(summary['incomplete_ticks'])}.",
             f"Duplicate / out-of-order ticks: {len(summary['duplicate_ticks'])} / {len(summary['out_of_order_ticks'])}.",
             f"Malformed profile lines / duplicate nodes: {summary['malformed_profile_lines']} / {summary['duplicate_node_lines']}.",
             f"Visible errors, resets or scheduler stops: {len(summary['errors_or_stops'])}.", ""]
    outside = summary["outside_actor_system"]
    if outside:
        lines += [f"Root CPU minus ActorSystem: mean **{outside['mean']:.3f}**, over "
                  f"{summary['ticks_with_actor_system']} ticks. This includes reporting and other work outside ActorSystem; "
                  "it is not an isolated logging cost.", ""]
    if summary["nodes"]:
        lines += ["## Observed profiling paths", "",
                  "| Path | Ticks present | Mean per sampled tick | Mean when present | p95 when present |",
                  "|---|---:|---:|---:|---:|"]
        largest = sorted(summary["nodes"].items(), key=lambda item: item[1]["mean_per_tick"], reverse=True)[:20]
        for path, node in largest:
            label = path.replace("|", "\\|")
            observed = node["total_when_present"]
            lines.append(f"| {label} | {node['ticks_present']} | {node['mean_per_tick']:.3f} | "
                         f"{observed['mean']:.3f} | {observed['p95']:.3f} |")
        lines += ["", "All paths and exclusive (`self`) CPU statistics are in summary.json.", ""]
    else:
        lines += ["No profiling tree was observed. Profiling may be disabled or its output filtered; "
                  "Root CPU summaries are still available.", ""]
    lines += ["## Interpretation", "",
              "- Profiling paths overlap: ActorSystem includes actor execution, and parents include their children. Do not sum these rows.",
              "- Missing paths contribute zero to mean_per_tick; filtered or lost logs can therefore understate costs.",
              "- CPU is sampled at the Root summary line, not after final log output or engine Memory serialization.",
              "- p95 uses the nearest rank; printed CPU values are already rounded by the bot.",
              "- Missing ticks can also be caused by lost logs; no visible stop does not prove empty queues.",
              "- Connection events, incomplete ticks and errors are preserved in summary.json.", ""]
    return "\n".join(lines)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path, help="UTF-8 log captured by capture_logs.py or screeps-cli --logs-only")
    parser.add_argument("--output-dir", type=Path,
                        help="Report directory; defaults to <log-stem>-analysis beside the log")
    args = parser.parse_args(argv)
    output = args.output_dir or args.log.with_name(args.log.stem + "-analysis")
    try:
        with args.log.open(encoding="utf-8-sig") as source:
            ticks, issues = parse_logs(source)
        summary = summarize(ticks, issues)
        output.mkdir(parents=True, exist_ok=True)
        for name, data in (("ticks.json", ticks), ("summary.json", summary)):
            (output / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (output / "report.md").write_text(markdown_report(summary), encoding="utf-8")
    except (OSError, UnicodeError, ValueError) as exc:
        print(f"Cannot analyze logs: {exc}", file=sys.stderr)
        return 1
    print(f"Analyzed {summary['ticks']} complete ticks: mean {summary['cpu']['mean']:.3f} CPU, "
          f"p95 {summary['cpu']['p95']:.3f}, max {summary['cpu']['max']:.3f}")
    print(f"Report: {(output / 'report.md').resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
