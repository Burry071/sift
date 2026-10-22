<!-- DRAFT — DEV x Hacktoberfest Week 3 "Slow and Steady" (#hf26challenge).
Challenge record closes 2026-10-24 11:59 PM PDT. Images are raw GitHub URLs. -->

---
title: "Sift: share it, sleep, it's filed — on-device Gemma, no cloud"
published: false
tags: devchallenge, hf26challenge, gemma, android
---

*This is my submission for the [Hacktoberfest Open-Source AI Challenge, Week 3: Slow and Steady](https://dev.to/challenges/hacktoberfest-week3-2026-10-19).*

**Share a thing. Put the phone down. It's filed by morning.**

Sift is a junk drawer that files itself. You share a link, a gate code, a recipe to it — two taps, zero decisions — and it closes. Overnight, while the phone charges, Gemma 4 reads every snippet you kept and sorts it into one of eight drawers with a short title of its own. In the morning it's a card catalog.

The app holds **no internet permission**. Not "we don't upload your notes" — the operating system forbids it. The model file is the only thing you ever download, and you hand it over yourself.

![Sift: share it, sleep, it's filed](https://raw.githubusercontent.com/Burry071/sift/main/post_images/0-cover.png)

## What I Built

The share sheet is where good intentions go to die. You see a gate code, a gift idea, a link you'll read later. You tap Share. Then you stare at a list of apps and remember what keeping it actually costs: open a notes app, make a note, paste, title it, pick a folder, decide where it belongs. You put the phone down instead. The link is gone by dinner.

Sift is the answer to that pause. Share the thing to Sift and walk away. The keep is instant; the sorting is deferred to a time when you are doing nothing anyway. Nothing happens while you watch. That is the product.

## Demo

One real run on an Android emulator: seven snippets pushed in through the same `ACTION_SEND` intent any app's share sheet fires, then one tap on **FILE NOW** because I was not going to wait until morning for a screenshot. Overnight is the same code path with a charging constraint — more on that below.

![Keep, file, read](https://raw.githubusercontent.com/Burry071/sift/main/post_images/2-flow.png)

*Left to right: the toast that closes the capture, the queue being sifted, and the catalog after the pass. The first two screens are from the same run; the third is the result.*

Six minutes forty-nine seconds later, all seven were filed: one card per drawer, each with the model's title over the text I actually sent. The seventh — *"Send me the file whenever you get a chance, no rush."* — is the one snippet with no obvious drawer, and it is sitting in INBOX. That is the fail-safe doing its job, not a bug.

![The fail-safe drawer](https://raw.githubusercontent.com/Burry071/sift/main/post_images/3-inbox.png)

## Try it yourself

Two downloads, once:

1. **[Sift v0.1.0 — signed arm64 APK](https://github.com/Burry071/sift/raw/main/apk/Sift-v0.1.0-arm64.apk)** (43 MB, Android 9+). Sideload it; there is no Play Store listing.
2. **The model file** — [litert-community/gemma-4-E2B-it](https://huggingface.co/litert-community/gemma-4-E2B-it), `gemma-4-E2B-it.litertlm`, about 2.6 GB. It is gated under Google's Gemma terms, so accept the license in your browser and download it yourself.

On first launch Sift shows one setup prompt: **Download the model** opens that page in your browser, and **I already have the file** hands the file over through the system picker. The first read compiles the model and takes a few minutes; after that, filing starts fast. The same prompt asks once whether Sift may post a notification while it files overnight. Declining changes nothing — the notification is a receipt, not a requirement.

![The setup prompt](https://raw.githubusercontent.com/Burry071/sift/main/post_images/1-setup.png)

After that, keeping a note is **Share → Sift** from any app.

## Code

[github.com/Burry071/sift](https://github.com/Burry071/sift) — Kotlin, Jetpack Compose, Room, WorkManager, LiteRT-LM. The signed APK is in the repo; build from source with `./gradlew :app:installDebug`.

The calibration corpus (40 invented snippets, labeled), every spike script, the image-composition script, and the raw per-entry results of every run in this post are committed under `corpus/`, `scripts/`, and the repo root. Nothing here is a vibes-based number; each table row is a file you can replay. There is no personal data in the repo: every snippet in the corpus and in the demo is invented.

## How I Built It

### The shape

Three layers, deliberately fewer than my last project:

1. **Keep (instant).** Any share lands verbatim in a local Room database, queued UNFILED. No model runs while you're holding the phone.
2. **File (later).** A WorkManager job reads each queued snippet with Gemma. Three votes per category at temperature 0.4; majority wins. If the votes disagree, or the answer isn't one of the eight labels, the note goes to INBOX. Then a title, with a fallback to the snippet's first line if the model rambles. One database write per snippet, so an interrupted night resumes where it stopped.
3. **Browse (morning).** One screen, set like a card catalog: drawers with counts, cards with the model's title over your verbatim text. The text is evidence, not paraphrase.

Two ways to trigger filing: the overnight periodic job, constrained to **charging + idle** (exactly the state your phone is in at 2 a.m.), or **FILE NOW**, a one-time request through the same worker with no constraints. The demo uses FILE NOW. The product uses both.

```
Requires: charging=true batteryNotLow=false deviceIdle=true
```

That line is `dumpsys jobscheduler` on the emulator, not a diagram of what I meant to build.

### The model

The shipping model is **Gemma 4 E2B IT** as a `.litertlm` file — 2,588,147,712 bytes, user-imported through the system file picker, run through **LiteRT-LM 0.18.1**. Android 9+, arm64, no INTERNET permission.

That sentence cost more than it looks. LiteRT-LM 0.18.1 shipped the week I built this; moving the app onto it dragged the Kotlin version to 2.4, which has no KSP release yet, which meant moving Room from KSP to kapt, which then needed a matching metadata reader on the annotation-processor classpath because Room's processor tops out at Kotlin metadata 2.3. None of that is interesting in the finished app. All of it was two evenings.

### The part that mattered: the runtime was lying

I calibrated the taxonomy on a corpus I wrote myself — 40 snippets, all invented, no personal data: 32 that clearly belong in one drawer, 8 written to be genuinely ambiguous. It is a calibration set, not a sample of the world. It tests the taxonomy, not my imagination's coverage of it.

The gate I set before running anything: **≥27 of 32 clear snippets exactly right, and ≥6 of 8 ambiguous ones at least acceptable.** Every run below is a committed result file with the raw votes.

| Run | Call path | Clear exact | Ambiguous ok | Malformed votes | Titles | Speed |
|---|---|---|---|---|---|---|
| v1, Gemma 3 1B | chat, zero-shot | 14/32 | 1/8 | 25/120 | 39/40 | 5.6 s/entry |
| v2, Gemma 3 1B | chat, few-shot turns | 0/32 | 0/8 | 118/120 | 39/40 | 8.5 s/entry |
| v3, Gemma 3 1B | + closed labels + answer line | 19/32 | 6/8 | 0/120 | 39/40 | 11.2 s/entry |
| v4, Gemma 3 1B | raw generate, grouped examples | 21/32 | 5/8 | 0/120 | 38/40 | 10.5 s/entry |
| v4b, Gemma 3 4B | same prompt, bigger model | 29/32 | 5/8 | 0/120 | 40/40 | 22 s/entry |
| v5, Gemma 4 E2B | conversation API | **30/32** | **7/8** | **0/120** | **40/40** | 23.6 s/entry |

The 4B speed is a median: one entry in that run took 15.9 hours because the laptop slept in the middle of it. Everything else is a mean over the run.

The v1–v3 1B runs are the usual small-model story: without a closed label list it invents categories ("LOCATION", "BUILD", "NETWORK"), and voting cannot save a model that is confidently wrong. The 4B cleared the clear-snippet gate easily (29/32) but missed the ambiguous gate by one (5/8 needed 6), and was only available as an Ollama tag — no `.litertlm` file, so it could never ship inside the app. The 4B row is the control, not the product.

Then Gemma 4 E2B arrived as a shippable file and the calibration fell apart in a way that made no sense. The same prompt that worked elsewhere produced empty strings, echoes of the note, and — once — a fabricated continuation of the prompt: `Note: "How to fix a leaky faucet"`. About one generation in six came back malformed. A model that could not answer one word, again and again.

The bug was not in the prompt. It was in the call path. The low-level session API (`run_prefill` / `run_decode`) continues raw text. On Gemma 4 it degenerates: the model was never shown its chat template, so "answer with one label" read as an invitation to continue the document. A **fresh conversation per call** — the API that applies the chat template — returns a clean one-word answer every time. Same weights, same prompt, same corpus: zero malformed votes across the run, **30/32 clear, 7/8 ambiguous, 40/40 titles.** A GO against the gate.

The shipping app now opens a fresh conversation for every generation, seeds every vote differently, and closes the conversation after each call.

The remaining misses are worth showing, not hiding. All three are unanimous wrong votes: "Anniversary plan: rooftop dinner" filed as EVENT instead of IDEA, "Tomorrow: submit the form, call the plumber, pick up the dry cleaning" as EVENT instead of LIST, and "The cafe wifi is down, use the one next door instead" as CODE instead of INBOX. Voting catches disagreement, not confident mistakes. INBOX is a fail-safe for uncertainty, not a guarantee that every drawer is right — which is why the text stays verbatim on every card and the drawers aren't hidden behind anything.

That is the "slow and steady" part of this week for me. The lazy product is built on a runtime detail that took a day of controlled comparisons to isolate.

## Why Does Open Innovation Matter?

Sift only works because the model is open-weight and local:

- **Zero marginal cost is what makes a junk drawer viable.** Nobody pays a subscription to file a gate code. If every snippet cost an API call, the honest business model for this app would be "don't build it". Local inference makes the cost of a filing run zero, forever.
- **Privacy is structural, not a promise.** The app holds no INTERNET permission. It is physically incapable of sending your notes anywhere. You bring the model file yourself; the app never downloads anything.
- **The model is a file, not a vendor.** Swapping to a better open-weight model is a file swap and a version bump, not a renegotiated contract. I moved from Gemma 3 1B to Gemma 4 E2B during this challenge and nobody's pricing page was involved.
- **A small model is enough because the problem is small.** The entire job is: read one note, pick one of eight labels, write five words. You don't need a frontier model for that. You need a model that runs on the phone you already own.

## My Agent Session

The session below is the actual working log from building Sift: the calibration runs, the runtime that lied, the emulator fight, the release build. It was captured in Qoder, which isn't one of DevRelay's session types — that list is fixed — so it is filed under Claude Code, the closest entry. The log is trimmed: private reasoning blocks and routine file reads are gone, but nothing in it is reconstructed.

{% agent_session 1090 %}

## The Lazy Payoff

The share interaction is **two taps and zero decisions**, counted on the emulator in the demo: tap Share in whatever app you're in, tap Sift in the sheet. Then the app closes itself and you're back where you were. (Three taps the very first time, while Android asks whether Sift may open shares by default.)

The notes-app version of the same save is five to seven taps and three decisions: open the app, make a note, paste, title it (what do I call this?), move it (where does it go?), pick the folder, come back. I counted that flow by hand — there's no instrument for another app's menus. The taps are the small part. The decisions are the reason the link dies in the share sheet.

The deeper saving is the one I actually wanted: **there is no organizing session to get around to.** The filing happens at 2 a.m. with zero taps and zero decisions, while the phone does something it was going to do anyway — charge. In the morning the work is simply done. The app's best feature is that you never watch it work.

On the emulator — an x86_64 box with no phone-class acceleration — the whole pass took **6 minutes 49 seconds**, from the FILE NOW tap to the last card: 58 seconds per snippet on average. That average is dominated by the first two while the 2.6 GB model paged in; once warm, snippets landed every 15 to 27 seconds. I don't own an arm64 Android device, so that is the only on-device number I have, and I'm not going to invent a phone's. The 2.6 GB model is imported once and stays in app storage.

## Prize Categories

**Best Use of Gemma.** Sift runs Gemma 4 E2B entirely on-device through LiteRT-LM, and the whole product is built around what a small open-weight model can do locally: read a note, choose a drawer, write a title — no network, no API key, no cloud.

---

Built for the DEV × Hacktoberfest Week 3 "Slow and Steady" challenge. The app does the work while you sleep; the week's work was making the runtime honest.
