#!/usr/bin/env python3
"""Spike v5: the shipping Gemma 4 E2B .litertlm file through LiteRT-LM.

Same corpus, same prompts and same vote rules as the app (Prompts.kt /
SiftPipeline.kt), so the desktop verdict is the verdict the phone will make.

Usage:
    python scripts/sift_test_v5.py <model.litertlm> [votes] [--limit N]

Writes results_sift_v5_<model-stem>.jsonl next to the repo root.
Gate (pre-registered): GO = >=27/32 exact on ids 1-32 AND >=6/8 acceptable
on ids 33-40, temp 0.4, 3 votes, majority of all votes cast, INBOX otherwise.
"""

import json
import pathlib
import random
import re
import sys
import time

import litert_lm

ANSWER_LINE = (
    "Answer with one word (ADDRESS, CODE, CONTACT, EVENT, IDEA, LINK, LIST, "
    "RECIPE, INBOX):"
)

CATEGORIZE = """You read a short note someone saved on their phone. Decide which drawer of a filing catalog it belongs to.

ADDRESS means a place or directions. Examples:
"Flat 4B, Rosewood Apartments, 12 Housing Board Road. Buzz 402 when you reach the gate." -> ADDRESS
"After the old bridge take the second left, then look for the white gate." -> ADDRESS

CODE means a password, wifi key, door code, promo code or license key. Examples:
"WiFi at my place: HomeNet_5G / Bh0ppz#24!q - case sensitive." -> CODE
"Door code is 4471 then #. If it beeps twice, call me." -> CODE
"Promo code SAVE40, works till Sunday, one use per account." -> CODE

CONTACT means a phone number, email, or how to reach a person. Examples:
"His number is +92 300 5550148. WhatsApp is the same." -> CONTACT
"Send the quote to sana.builds@example.com before Friday." -> CONTACT

EVENT means a plan with a date or time, an appointment, a pickup or arrival. Examples:
"Dinner Friday 8pm at the Thai place near your office. Usual crowd." -> EVENT
"Flight lands 11:35pm Tuesday, Terminal 3. Wait at arrivals." -> EVENT

IDEA means a gift idea or something to get or do someday. Examples:
"Gift for mom: the blue ceramic tea set she kept staring at in the bazaar." -> IDEA
"Get the terracotta planters from the nursery before they sell out again." -> IDEA

LINK means something to read or watch later. Examples:
"Watch this later - the documentary about city rivers: example.com/rivers-film" -> LINK
"Good explainer on how transformers actually work: example.com/attention-explained" -> LINK

LIST means items to buy, pack, or do. Examples:
"Milk, eggs, basmati rice, ginger, two lemons, dish soap." -> LIST
"Packing: charger, universal adapter, meds, passport copy, one warm layer." -> LIST

RECIPE means food or cooking instructions. Examples:
"Cold brew: 1 cup coarse grounds to 4 cups water, 12 hours in the fridge, strain twice." -> RECIPE
"The chutney needs fresh mint, not dried. Squeeze half a lemon at the end." -> RECIPE

INBOX means none of the drawers genuinely fit. Examples:
"Send me the file whenever you get a chance, no rush." -> INBOX
"Haha nice one, see you at the thing" -> INBOX

Addresses, codes, contacts, plans, gift ideas, links to read or watch, lists, and cooking notes each have their own drawer. If you are not sure, answer INBOX.

Note: "{text}"
""" + ANSWER_LINE

TITLE = """You write very short titles for saved notes. Reply with a title of 3 to 6 words, plain text, no quotes, no trailing punctuation. Reply with the title only.

Note: "{text}"
Title:"""

LABEL_RE = re.compile(r"\b(ADDRESS|CODE|CONTACT|EVENT|IDEA|LINK|LIST|RECIPE|INBOX)\b")
NOTE_PREFIX_RE = re.compile(r"^note\s*:", re.IGNORECASE)
META_WORD_RE = re.compile(r"\b(label|category|assistant)\b", re.IGNORECASE)


def parse_label(raw):
    m = LABEL_RE.search(raw.upper())
    return m.group(1) if m else None


def majority(votes):
    counts = {}
    for v in votes:
        if v is not None:
            counts[v] = counts.get(v, 0) + 1
    if not counts:
        return "INBOX"
    best = max(counts.items(), key=lambda kv: kv[1])
    return best[0] if best[1] >= (len(votes) + 1) // 2 else "INBOX"


def title_degenerate(title, text):
    if not title or len(title) > 60:
        return True
    if NOTE_PREFIX_RE.search(title):
        return True
    if title.lower() == text.lower()[:len(title)]:
        return True
    if META_WORD_RE.search(title):
        return True
    return False


def fallback_title(text):
    first = text.strip().splitlines()[0] if text.strip() else ""
    return first if len(first) <= 48 else first[:47] + "\u2026"


def message_text(message):
    parts = getattr(getattr(message, "contents", None), "contents", None) or []
    return "".join(getattr(p, "text", "") or "" for p in parts)


# The low-level prefill/decode session continues raw text and degenerates on
# Gemma 4 (empty or echoed notes). A fresh conversation per call applies the
# model's chat template and returns a clean answer, so that is the shipping path.
def generate(engine, prompt, max_tokens, temperature):
    conversation = engine.create_conversation(
        sampler_config=litert_lm.SamplerConfig(
            top_k=40, top_p=0.95, temperature=temperature,
            seed=random.randint(0, 2**31 - 1),
        ),
        max_output_tokens=max_tokens,
    )
    try:
        return message_text(conversation.send_message(prompt))
    finally:
        conversation.close()


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    limit = None
    for a in sys.argv[1:]:
        if a.startswith("--limit"):
            limit = int(a.split("=", 1)[1]) if "=" in a else None
    model_path = pathlib.Path(args[0]).resolve()
    votes = int(args[1]) if len(args) > 1 else 3
    if limit is None and len(args) > 2:
        limit = int(args[2])

    root = pathlib.Path(__file__).resolve().parent.parent
    corpus = [json.loads(line) for line in
              (root / "corpus" / "snippets_v1.jsonl").read_text(encoding="utf-8").splitlines()
              if line.strip()]
    if limit:
        corpus = corpus[:limit]

    out = root / f"results_sift_v5_{model_path.stem}.jsonl"
    print(f"model: {model_path.name} ({model_path.stat().st_size / 1e6:.1f} MB)")
    print(f"corpus: {len(corpus)} entries, {votes} votes, temp 0.4, ALL SNIPPETS INVENTED")
    print(f"output: {out}")

    t_load = time.time()
    engine = litert_lm.Engine(str(model_path), max_num_tokens=1024)
    print(f"engine ready in {time.time() - t_load:.1f} s")

    t_run = time.time()
    with open(out, "w", encoding="utf-8") as f:
        for entry in corpus:
            text = entry["text"]
            model_text = text[:2000]
            raw_votes, parsed = [], []
            t0 = time.time()
            for _ in range(votes):
                raw = generate(engine, CATEGORIZE.format(text=model_text), 12, 0.4)
                raw_votes.append(raw)
                parsed.append(parse_label(raw))
            category = majority(parsed)
            title_raw = generate(engine, TITLE.format(text=model_text), 16, 0.4).strip()
            fallback = title_degenerate(title_raw, text)
            title = fallback_title(text) if fallback else title_raw
            secs = time.time() - t0
            row = {
                "id": entry["id"],
                "true": entry["cat"],
                "accept": entry["accept"],
                "text": text,
                "votes": raw_votes,
                "parsed": parsed,
                "pred": category,
                "exact": category == entry["cat"],
                "acceptable": category in entry["accept"],
                "title": title,
                "title_raw": title_raw,
                "title_fallback": fallback,
                "secs": round(secs, 2),
            }
            f.write(json.dumps(row, ensure_ascii=False) + "\n")
            f.flush()
            print(f"  id {entry['id']:>2} true={entry['cat']:<8} pred={category:<8} "
                  f"{'OK ' if row['acceptable'] else 'MISS'} votes={parsed} {secs:.1f}s")

    total = time.time() - t_run
    rows = [json.loads(line) for line in out.read_text(encoding="utf-8").splitlines() if line.strip()]
    clear = [r for r in rows if r["id"] <= 32]
    ambig = [r for r in rows if r["id"] > 32]
    c_exact = sum(r["exact"] for r in clear)
    a_accept = sum(r["acceptable"] for r in ambig)
    titles = sum(not r["title_fallback"] for r in rows)
    malformed = sum(v is None for r in rows for v in r["parsed"])
    print()
    print(f"corpus={len(rows)} ({len(clear)} clear + {len(ambig)} ambiguous), ALL SNIPPETS INVENTED")
    print(f"clear:     exact {c_exact}/{len(clear)}")
    print(f"ambiguous: acceptable {a_accept}/{len(ambig)}")
    print(f"malformed: {malformed}/{len(rows) * votes}")
    print(f"titles kept: {titles}/{len(rows)}")
    print(f"speed: {total:.1f} s total, {total / len(rows):.1f} s/entry")
    if len(rows) < 40:
        print("GATE: PARTIAL RUN - not judged (need the full 40-entry corpus)")
        return
    go = c_exact >= 27 and a_accept >= 6
    print(f"GATE: clear>={c_exact >= 27} ({c_exact}/32 vs 27), "
          f"ambig>={a_accept >= 6} ({a_accept}/8 vs 6) -> {'GO' if go else 'NO-GO'}")


if __name__ == "__main__":
    main()
