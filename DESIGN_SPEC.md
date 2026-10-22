# Sift — DESIGN SPEC (v1)

The junk drawer that files itself. Share anything worth keeping into Sift from
any app — two taps, phone away. Overnight, while the phone charges, a small
on-device Gemma reads each snippet and files it: category drawer + short title.
In the morning the drawer is organized.

Entry for DEV x Hacktoberfest Week 3 "Slow and Steady" (challenge id=81,
tag #hf26challenge). Deadline Sat 2026-10-25 11:59 PM PDT.

## The user contract

- **Keeping something:** Share -> Sift. Toast: "Kept. Filed overnight."
  2 taps, 0 decisions. This is the lazy-payoff headline.
- **Filing:** happens without the user. WorkManager job, constraints:
  charging + idle. Typically fires when the phone is plugged in overnight.
- **Morning:** open Sift -> card catalog, sorted, titled.
- **Impatient path:** FILE NOW button drains the queue immediately
  (also the demo path, since you can't wait a night for a screenshot).

## The stack

Same base stack as Gazette (Week 2): Kotlin, Jetpack Compose, Room.
Upgraded for Gemma 4: Kotlin 2.4.21 (AGP 8.6.0, Gradle 8.8), Room 2.8.5
via **kapt** (KSP has no Kotlin 2.4 release yet), LiteRT-LM **0.18.1**
(Gemma 4 support first landed in 0.17.0). Model: **Gemma 4 E2B IT**
(.litertlm, 2,588,147,712 bytes ≈ 2.6 GB, user-imported via SAF picker —
same flow as Gazette: GET THE FILE opens the HF page, I HAVE THE FILE
opens the system file picker). No INTERNET permission. Android 9+, arm64.

Runtime note: Gemma 4 cannot be driven through the low-level session
prefill/decode path — it returns empty strings, echoes the note, or
fabricates prompt continuations. The app uses the Conversation API, which
applies the model's chat template, with a fresh conversation per call.

## Pipeline (3 layers — deliberately simpler than Gazette's 5)

1. **Sanity gate (0 ms).** Empty/whitespace text is dropped on arrival
   (never stored). Everything else is kept — the user chose to share it,
   so unlike Gazette there is no junk-vs-real classification. Oversized
   text (>2,000 chars) is truncated for the model only; the stored text
   is always verbatim.
2. **Category vote.** One-word label from a fixed taxonomy:
   ADDRESS, CODE, CONTACT, EVENT, IDEA, LINK, LIST, RECIPE.
   Few-shot prompt (one example per label, alternating chat turns).
   3 samples, temp 0.4, majority >= 2 wins. Disagreement -> INBOX,
   the fail-safe drawer at the bottom. Never a crash, never a guess:
   worst case is a snippet sitting in INBOX for a two-tap manual move.
3. **Title.** 1 sample, "3 to 6 words". Degenerate-title check
   (empty, > 60 chars, echoes the note prefix, contains meta words)
   -> fallback = first line of the snippet, <= 48 chars + ellipsis.

**No reconciliation layer, and that is the design point.** Gazette needed
one because a wrong amount is a wrong ledger. Sift's failure mode is soft:
a snippet in the wrong drawer costs two taps. The vote + INBOX fail-safe
is the whole safety story. (Saying this honestly in the post contrasts
with Week 2's "engineering compensates for the model" — here the model
size doesn't need compensating for, the domain does.)

## Data model (Room)

`snippets` table:
- `id` PK autoincrement
- `text` TEXT NOT NULL (verbatim, evidence)
- `source_app` TEXT NULL (share-source package label)
- `created_at` INTEGER (epochMs, when shared)
- `filed_at` INTEGER NULL (epochMs, when the model finished it)
- `status` TEXT ('UNFILED' | 'FILED')
- `category` TEXT NULL (drawer label or INBOX)
- `title` TEXT NULL
- `title_is_fallback` INTEGER (boolean)

## Components

- `ShareActivity`: manifest intent-filter ACTION_SEND + ACTION_SEND_TEXT,
  mimeType text/plain. Persist + toast + finish(). No UI surface.
- `FilingWorker` (CoroutineWorker): drains UNFILED rows sequentially,
  one model pipeline per row, DB update per row (partial progress persists).
  `setForeground(dataSync)` — visible "Sift is filing while you sleep"
  notification; required for > 10 min of background work; foreground
  service type `dataSync` declared in manifest (API 34+ requirement),
  permissions FOREGROUND_SERVICE + FOREGROUND_SERVICE_DATA_SYNC.
  Enqueued as: periodic work (charging + idle + battery-not-low) and
  by FILE NOW (one-time, no constraints, same code path).
- `ModelRuntime`: LiteRT-LM engine, load from filesDir `model.litertlm`,
  status flow MISSING -> LOADING -> READY, fresh Session per generate
  call with randomized seed (vote diversity — proven in Gazette).
- `CatalogScreen`: the card catalog (see DESIGN.md).

## Calibration (archived runs, replayable)

Corpus: 40 snippets, ALL INVENTED (no personal data — standing privacy
rule; this app's real captures stay on the user's phone by design).
32 clear (4 per category) + 8 deliberately ambiguous (acceptable sets).
Scripts and raw results committed: `scripts/sift_test_v4.js` (v1-v4,
Ollama) and `scripts/sift_test_v5.py` (v5, LiteRT-LM, the shipping
runtime); results in `results_sift_v*.jsonl`.

Gate (pre-registered): GO = >=27/32 exact on clear, >=6/8 acceptable on
ambiguous. Every run archived with raw votes in `results_sift_v*.jsonl`.
v1-v4 ran on Ollama CPU (num_gpu:0); v5 ran on LiteRT-LM 0.18.1 with the
shipping `.litertlm` file. 3 votes at temp 0.4 throughout.

| Run | Prompt / call path | Clear exact | Ambig. acceptable | Malformed | Titles kept | Speed |
|-----|--------------------|-------------|-------------------|-----------|-------------|-------|
| v1 | 1B, zero-shot, /api/chat, no examples | 14/32 | 1/8 | 25/120 | 39/40 | 5.6 s/entry |
| v2 | 1B, few-shot turns, label list removed | 0/32 | 0/8 | 118/120 | 39/40 | 8.5 s/entry |
| v3 | 1B, v2 + closed label list + answer line | 19/32 | 6/8 | 0/120 | 39/40 | 11.2 s/entry |
| v4 | 1B, raw /api/generate, grouped examples | 21/32 | 5/8 | 0/120 | 38/40 | 10.5 s/entry |
| v4b | 4B, same prompt (Ollama only) | 29/32 | 5/8 | 0/120 | 40/40 | 22 s/entry (median) |
| v5s | Gemma 4 E2B, low-level session prefill/decode | 24/32 | 7/8 | 21/120 | 32/40 | 29.2 s/entry |
| v5c | Gemma 4 E2B, conversation API | **30/32** | **7/8** | **0/120** | **40/40** | 23.6 s/entry |

v2 lesson: without the explicit closed label list the 1B invents labels
(LOCATION, BUILD, ROUTE, NETWORK) - the reminder adjacent to the answer
slot is load-bearing. v4 lesson: prompt format was not the bottleneck;
9 of 11 v4 misses were UNANIMOUS wrong votes, so the model is confidently
wrong and neither voting nor prompt tuning rescues it.

1B verdict: NO-GO at 21/32. The 4B cleared the clear gate (29/32) but
missed the ambiguous gate by one (5/8), and has no `.litertlm` file, so
it could never ship on-device (recorded as `results_sift_v4_gemma3_4b.jsonl`;
speed is the median - one entry took 15.9 h because the laptop slept).

v5 (shipping model) first ran through the low-level session API:
24/32 with 21/120 malformed votes (empty outputs, echoed notes, fabricated
prompt continuations). The failure was the call path, not the prompt or
the weights: raw prefill never applies the chat template. A fresh
conversation per call (chat template applied) is the shipping path and is
a GO: 30/32 clear, 7/8 ambiguous, 0 malformed, 40/40 titles.

Residual honest gaps: the two clear misses (id 20 "Anniversary plan:
rooftop dinner" -> EVENT; id 27 "Tomorrow: submit the form, call the
plumber..." -> EVENT) and the one ambiguous miss (id 38 "The cafe wifi is
down" -> CODE instead of INBOX) are all UNANIMOUS wrong votes. Voting
catches disagreement, not confident mistakes, so INBOX is a fail-safe for
uncertainty, not a guarantee that every drawer is right.

## Demo script (for the post)

1. From any app, share 7 snippets of different kinds (one deliberately
   ambiguous). Reproducible on emulator (note: EXTRA_TEXT key is
   case-sensitive; lowercase `text` silently arrives empty and the share
   is dropped):
   `adb shell am start -a android.intent.action.SEND -t text/plain \
      --es android.intent.extra.TEXT 'Buy: candles, cardamom' \
      -n com.sift.app/com.sift.service.ShareActivity`
2. Show the toast contract + the UNFILED count on the catalog screen.
3. Tap FILE NOW -> SIFTING pulse row -> drawers fill one by one.
4. Final catalog: sections with counts, titles + verbatim text; the
   ambiguous snippet either lands in INBOX or is shown where it went.
5. Lazy payoff: count the taps. Share-into-Sift = 2 taps, 0 decisions,
   counted on the emulator (3 on the very first share, while Android asks
   "Just once / Always"). The notes-app equivalent of the same save
   (open, new note, paste, title, move, pick folder, back) is five to
   seven taps and three decisions; it is a counted flow, not a measured
   on-device number.

### Measured result of the demo run (2026-10-21)

- FILE NOW tapped 18:54:59, last snippet written 19:01:48 -> 409 s for
  7 snippets, 58 s/snippet mean. The first two (145 s, 171 s) carry the
  model paging in; warm entries landed at 15-27 s each.
- Final drawers: ADDRESS 1, CODE 1, CONTACT 1, EVENT 1, IDEA 0, LINK 1,
  LIST 0, RECIPE 1, INBOX 1. The ambiguous "Send me the file whenever you
  get a chance, no rush." went to INBOX, which is the intended fail-safe
  outcome, not a miss.
- Screenshots: shot_catalog_top.png, shot_catalog_bottom.png,
  shot_demo_02_toast.png, shot_demo_04_sifting.png, shot_setup_prompt.png;
  composed into post_images/0-cover..3-inbox by scripts/make_post_images.py.

## Risks and levers

- **Category accuracy below gate on the shipped runtime** -> levers in
  order: conversation-API call path (applied — session prefill degenerates
  on Gemma 4), shrink taxonomy (8 -> 5: merge CODE+CONTACT, IDEA+LINK),
  Gemma 3 4B (29/32 clear, but ships only as an Ollama tag — no
  `.litertlm` file to import, so it cannot be the on-device model).
- **Overnight job doesn't fire on emulator** -> FILE NOW is the demo
  path; overnight behavior described honestly in the post (constraint
  config shown, real-device night run if the Pixel 7 is available).
- **Model RAM while foreground service runs** (~600 MB+) -> sequential
  one-at-a-time processing, no batching, release session per call.
- **Title junk** -> fallback rule, already in the spike.

## Success criteria (v1, by Thu 2026-10-23 night)

- All 7 demo snippets land in an acceptable drawer end-to-end on the
  emulator (share -> queue -> FILE NOW -> catalog).
- Overnight worker fires on a charging+idle emulator state (or its
  non-firing honestly documented + FILE NOW shown).
- Unfiled -> filed transition visible with SIFTING pulse; no crashes.
- Signed arm64 APK installs.
- If behind schedule: cut tags, cut overnight batching, keep
  share -> categorize -> sorted catalog.
