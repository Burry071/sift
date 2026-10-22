#!/usr/bin/env python3
"""Submit the curated session to DevRelay over the local stdio MCP server."""
import json
import pathlib
import subprocess
import sys
import threading

EXE = r"C:\Users\User\.devrelay\bin\dev_mlh_mcp_server.exe"
PAYLOAD = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "session_compact.json")
TOOL_NAME = sys.argv[2] if len(sys.argv) > 2 else "qoder"


def send(proc, msg):
    proc.stdin.write(json.dumps(msg) + "\n")
    proc.stdin.flush()


def main():
    payload = json.loads(PAYLOAD.read_text(encoding="utf-8"))
    proc = subprocess.Popen([EXE, "--stdio"], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, encoding="utf-8", bufsize=1)
    send(proc, {"jsonrpc": "2.0", "id": 1, "method": "initialize",
                "params": {"protocolVersion": "2024-11-05", "capabilities": {},
                           "clientInfo": {"name": "sift-post", "version": "1.0"}}})
    init = json.loads(proc.stdout.readline())
    print("init:", json.dumps(init)[:200], file=sys.stderr)
    send(proc, {"jsonrpc": "2.0", "method": "notifications/initialized"})
    send(proc, {"jsonrpc": "2.0", "id": 2, "method": "tools/call",
                "params": {"name": "submit_agent_session",
                           "arguments": {"title": payload["title"],
                                         "tool_name": TOOL_NAME,
                                         "curated_data": payload["curated_data"]}}})
    line = proc.stdout.readline()
    print(line)
    proc.terminate()
    try:
        err = proc.stderr.read()
        if err.strip():
            print("stderr:", err[-800:], file=sys.stderr)
    except Exception:
        pass


if __name__ == "__main__":
    main()
