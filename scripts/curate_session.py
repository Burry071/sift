#!/usr/bin/env python3
"""Build a DevRelay agent-session payload from the real Qoder transcript.

Keeps human turns, assistant narration, and the tool calls that carry the
story (spike runs, builds, device checks); drops private reasoning
(thinking blocks) and routine file reads.
"""
import json
import pathlib
import re
import sys

SESSION = pathlib.Path(
    r"C:\Users\User\.qoder\projects\C--Users-User-Documents-Qoder-2026-10-13-ec4b45db"
    r"\ec4b45db-cd38-4c2e-8949-0bfa57ec17b7.jsonl")
SIFT_START = "2026-10-20T13:52:00"

KEEP_CMD = re.compile(
    r"sift_test|assembleRelease|assembleDebug|installDebug|dumpsys jobscheduler"
    r"|apksigner|screencap|sift\.db|sha256sum|probe_template|litert", re.I)
MAX_TEXT = 420
MAX_OUT = 300
MAX_CALLS = 45


def scrub(s):
    for a, b in [
        (r"C:\\Users\\User", "~"),
        (r"C:/Users/User", "~"),
        (r"D:/trailwalk", "<models>"),
        (r"D:\\trailwalk", "<models>"),
        (r"D:/Temp", "<tmp>"),
        (r"C:\\Users\\User\\Documents\\Qoder\\2026-10-13\\sift", "<repo>"),
    ]:
        s = re.sub(a, b, s)
    return s


def clip(s, n):
    s = (s or "").strip()
    return s if len(s) <= n else s[: n - 1].rstrip() + "…"


def main():
    rows = []
    with SESSION.open(encoding="utf-8", errors="replace") as f:
        for line in f:
            try:
                o = json.loads(line)
            except json.JSONDecodeError:
                continue
            ts = str(o.get("timestamp") or "")
            if ts < SIFT_START:
                continue
            if o.get("type") not in ("user", "assistant"):
                continue
            rows.append(o)

    # tool_use_id -> output text, so calls can carry their results
    outputs = {}
    for o in rows:
        if o.get("type") != "user":
            continue
        for b in o.get("message", {}).get("content") or []:
            if isinstance(b, dict) and b.get("type") == "tool_result":
                c = b.get("content")
                if isinstance(c, list):
                    c = "".join(x.get("text", "") for x in c if isinstance(x, dict))
                outputs[b.get("tool_use_id")] = clip(scrub(str(c)), MAX_OUT)

    messages = []
    kept_calls = 0
    last_cmd = ""
    for o in rows:
        content = o.get("message", {}).get("content") or []
        if isinstance(content, str):
            content = [{"type": "text", "text": content}]
        if o.get("type") == "user":
            texts = [b.get("text", "") for b in content
                     if isinstance(b, dict) and b.get("type") == "text"]
            text = "\n".join(t for t in texts if t).strip()
            if not text or "This session is being continued" in text:
                continue
            if text.startswith("new challange"):
                text = text.replace("new challange", "", 1).strip()
            messages.append({"role": "user",
                             "content": [{"type": "text", "text": clip(scrub(text), 2500)}]})
        else:
            blocks = []
            for b in content:
                if not isinstance(b, dict):
                    continue
                if b.get("type") == "text" and b.get("text", "").strip():
                    blocks.append({"type": "text", "text": clip(scrub(b["text"]), MAX_TEXT)})
                elif b.get("type") == "tool_use":
                    if kept_calls >= MAX_CALLS:
                        continue
                    name = b.get("name", "tool")
                    inp = b.get("input") or {}
                    cmd = str(inp.get("command") or inp.get("description") or "")
                    if name != "Bash" or not KEEP_CMD.search(cmd):
                        continue
                    if cmd[:80] == last_cmd:
                        continue
                    last_cmd = cmd[:80]
                    kept_calls += 1
                    blocks.append({
                        "type": "tool_call",
                        "name": name,
                        "input": clip(scrub(cmd), 500),
                        "output": outputs.get(b.get("id"), ""),
                    })
            if blocks:
                messages.append({"role": "assistant", "content": blocks})

    payload = {
        "title": "Sift — share anything, file it overnight (DEV x Hacktoberfest Week 3)",
        "tool_name": "qoder",
        "curated_data": {
            "messages": messages,
            "metadata": {
                "tool_name": "qoder",
                "session_id": "ec4b45db-cd38-4c2e-8949-0bfa57ec17b7",
                "total_messages": len(messages),
            },
        },
    }
    out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "session_payload.json")
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
    tool_calls = sum(1 for m in messages for b in m["content"] if b["type"] == "tool_call")
    print(f"{out}: {len(messages)} messages, {tool_calls} tool calls, {out.stat().st_size/1024:.0f} KB")


if __name__ == "__main__":
    main()
