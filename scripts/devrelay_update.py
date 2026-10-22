#!/usr/bin/env python3
"""Update the Sift DEV article from POST.md through the local DevRelay MCP server.

Usage:  python scripts/devrelay_update.py [--publish]
"""
import json
import pathlib
import subprocess
import sys

EXE = r"C:\Users\User\.devrelay\bin\dev_mlh_mcp_server.exe"
ROOT = pathlib.Path(__file__).resolve().parent.parent
ARTICLE_ID = 4890076
TITLE = "Sift: share it, sleep, it's filed — on-device Gemma, no cloud"
MAIN_IMAGE = "https://raw.githubusercontent.com/Burry071/sift/main/post_images/0-cover.png"
DESCRIPTION = (
    "A junk drawer that files itself: two taps to keep anything, then an "
    "on-device Gemma sorts it overnight. No internet permission, no accounts, no cloud."
)


def send(proc, msg):
    proc.stdin.write(json.dumps(msg) + "\n")
    proc.stdin.flush()


def main():
    publish = "--publish" in sys.argv
    md = (ROOT / "POST.md").read_text(encoding="utf-8")
    body = md.split("---", 2)[2].lstrip("\n")
    args = {
        "id": ARTICLE_ID,
        "title": TITLE,
        "body_markdown": body,
        "tags": ["devchallenge", "hf26challenge", "gemma", "android"],
        "description": DESCRIPTION,
        "main_image": MAIN_IMAGE,
        "ai_disclosure_level": "some_ai",
        "published": publish,
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
                "params": {"name": "update_article", "arguments": args}})
    response = proc.stdout.readline()
    proc.terminate()
    (ROOT / "devrelay_update_response.json").write_text(response, encoding="utf-8")
    print(response[:300].encode("ascii", "replace").decode("ascii"))


if __name__ == "__main__":
    main()
