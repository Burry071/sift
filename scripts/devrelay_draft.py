#!/usr/bin/env python3
"""Create the DEV draft from POST.md through the local DevRelay MCP server."""
import json
import pathlib
import subprocess
import sys

EXE = r"C:\Users\User\.devrelay\bin\dev_mlh_mcp_server.exe"
ROOT = pathlib.Path(__file__).resolve().parent.parent


def send(proc, msg):
    proc.stdin.write(json.dumps(msg) + "\n")
    proc.stdin.flush()


def main():
    md = (ROOT / "POST.md").read_text(encoding="utf-8")
    title = "Sift — share anything, wake up to a sorted drawer, entirely on your phone"
    body = md.split("---", 2)[2].lstrip("\n")
    args = {
        "title": title,
        "body_markdown": body,
        "tags": ["devchallenge", "hf26challenge", "gemma", "android"],
        "published": False,
        "description": ("Keep anything with two taps and zero decisions; overnight, "
                        "Gemma 4 E2B files each snippet into a drawer on-device. No "
                        "INTERNET permission, no cloud, no organizing session."),
    }
    proc = subprocess.Popen([EXE, "--stdio"], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, encoding="utf-8", bufsize=1)
    send(proc, {"jsonrpc": "2.0", "id": 1, "method": "initialize",
                "params": {"protocolVersion": "2024-11-05", "capabilities": {},
                           "clientInfo": {"name": "sift-post", "version": "1.0"}}})
    proc.stdout.readline()
    send(proc, {"jsonrpc": "2.0", "method": "notifications/initialized"})
    send(proc, {"jsonrpc": "2.0", "id": 2, "method": "tools/call",
                "params": {"name": "create_article", "arguments": args}})
    print(proc.stdout.readline())
    proc.terminate()


if __name__ == "__main__":
    main()
