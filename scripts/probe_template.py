#!/usr/bin/env python3
"""Probe: which LiteRT-LM call style gives clean one-word answers for Gemma 4.

Compares raw prefill/decode (template off), templated session, and the
high-level Conversation API on a few corpus entries.
"""
import json
import pathlib
import random
import sys

import litert_lm
from sift_test_v5 import CATEGORIZE, TITLE

MODEL = sys.argv[1]
IDS = [int(x) for x in sys.argv[2].split(",")] if len(sys.argv) > 2 else [11, 16, 23, 24]

root = pathlib.Path(__file__).resolve().parent.parent
corpus = {e["id"]: e for e in (json.loads(l) for l in
          (root / "corpus" / "snippets_v1.jsonl").read_text(encoding="utf-8").splitlines() if l.strip())}

sampler = lambda: litert_lm.SamplerConfig(top_k=40, top_p=0.95, temperature=0.4,
                                          seed=random.randint(0, 2**31 - 1))

engine = litert_lm.Engine(MODEL, max_num_tokens=1024)


def raw(text, max_tokens=12, template=False):
    s = engine.create_session(sampler_config=sampler(), max_output_tokens=max_tokens,
                              apply_prompt_template=template)
    try:
        s.run_prefill([text])
        return "".join(s.run_decode().texts or [])
    finally:
        s.close()


def conv(text, max_tokens=12):
    c = engine.create_conversation(sampler_config=sampler(), max_output_tokens=max_tokens,
                                   system_message=None)
    try:
        m = c.send_message(text)
        return m.get("content", "") if isinstance(m, dict) else str(m)
    finally:
        c.close()


for i in IDS:
    e = corpus[i]
    p = CATEGORIZE.format(text=e["text"])
    t = TITLE.format(text=e["text"])
    print(f"=== id {i} true={e['cat']}")
    print("  raw-off :", repr(raw(p)))
    print("  raw-on  :", repr(raw(p, template=True)))
    print("  conv    :", repr(conv(p)))
    print("  title off:", repr(raw(t, max_tokens=16)))
    print("  title conv:", repr(conv(t, max_tokens=16)))
