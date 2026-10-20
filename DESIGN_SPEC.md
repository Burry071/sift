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

Same proven stack as Gazette (Week 2): Kotlin, Jetpack Compose, Room,
LiteRT-LM 0.16.1 (pinned — 0.17+ needs Kotlin 2.4, no KSP for it),
Gemma 3 1B int4 (.litertlm, ~557 MB, user-imported via SAF picker —
same flow as Gazette: GET THE FILE opens the HF page, I HAVE THE FILE
opens the system file picker). No INTERNET permission. Android 9+, arm64.

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
Scripts and raw results committed: `scripts/sift_test.js` (v1 zero-shot),
`scripts/sift_test_v2.js` (v2 few-shot), `results_sift_v*.jsonl`.

<!-- CALIBRATION_TABLE: filled from archived v1/v2 runs, measured only -->

## Demo script (for the post)

1. From any app, share 6 snippets of different kinds (adb `am start`
   ACTION_SEND for reproducibility on emulator).
2. Show the toast contract + the UNFILED count on the catalog screen.
3. Tap FILE NOW -> SIFTING pulse row -> drawers fill one by one.
4. Final catalog: sections with counts, titles + verbatim text,
   the ambiguous snippet visible in INBOX.
5. Lazy payoff: count the taps. Share-into-Sift = 2 taps, 0 decisions.
   Manual equivalent (Keep + folder + title) = 12+ taps, 3 decisions.
   Numbers measured on-device in the demo, not estimated.

## Risks and levers

- **1B category accuracy below gate** -> levers in order: few-shot v2
  (applied), shrink taxonomy (8 -> 5: merge CODE+CONTACT, IDEA+LINK),
  Gemma 3 4B as the imported model (3.3 GB, same LiteRT-LM path).
- **Overnight job doesn't fire on emulator** -> FILE NOW is the demo
  path; overnight behavior described honestly in the post (constraint
  config shown, real-device night run if the Pixel 7 is available).
- **Model RAM while foreground service runs** (~600 MB+) -> sequential
  one-at-a-time processing, no batching, release session per call.
- **Title junk** -> fallback rule, already in the spike.

## Success criteria (v1, by Thu 2026-10-23 night)

- All 6 demo snippets land in an acceptable drawer end-to-end on the
  emulator (share -> queue -> FILE NOW -> catalog).
- Overnight worker fires on a charging+idle emulator state (or its
  non-firing honestly documented + FILE NOW shown).
- Unfiled -> filed transition visible with SIFTING pulse; no crashes.
- Signed arm64 APK installs.
- If behind schedule: cut tags, cut overnight batching, keep
  share -> categorize -> sorted catalog.
