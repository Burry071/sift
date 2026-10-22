---
name: Sift Card Catalog
description: The junk drawer that files itself — a phone card catalog, typeset like a library finding aid. Daylight-only v1.
colors:
  paper: "#F6F8F4"
  ink: "#161A17"
  ink-soft: "#5E6660"
  hairline: "#E0E4DC"
  pine: "#0E4F34"
  pine-wash: "#EAF0EB"
typography:
  wordmark: { fontFamily: Schibsted Grotesk, fontSize: 28px, fontWeight: 700, letterSpacing: -0.01em, lineHeight: 1.1 }
  status: { fontFamily: Schibsted Grotesk, fontSize: 13px, fontWeight: 400, lineHeight: 1.4, letterSpacing: 0.01em }
  drawer-label: { fontFamily: Schibsted Grotesk, fontSize: 12px, fontWeight: 600, letterSpacing: 0.14em, lineHeight: 1.2 }
  count: { fontFamily: Schibsted Grotesk, fontSize: 12px, fontWeight: 500, lineHeight: 1.2 }
  snippet-title: { fontFamily: Schibsted Grotesk, fontSize: 16px, fontWeight: 500, lineHeight: 1.3, letterSpacing: -0.005em }
  snippet-text: { fontFamily: Schibsted Grotesk, fontSize: 13px, fontWeight: 400, lineHeight: 1.45, letterSpacing: 0.005em }
  button: { fontFamily: Schibsted Grotesk, fontSize: 13px, fontWeight: 600, letterSpacing: 0.10em, lineHeight: 1.2 }
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
rounded:
  none: 0px
components:
  drawer-section:
    padding: "{spacing.md}"
    backgroundColor: "{colors.paper}"
  snippet-row:
    padding: "{spacing.md}"
    backgroundColor: "{colors.paper}"
  accent-pill:
    backgroundColor: "{colors.pine-wash}"
    textColor: "{colors.pine}"
    rounded: "{rounded.none}"
    padding: "{spacing.sm}"
---

# Sift — Card Catalog

## Overview

Sift is a junk drawer that files itself. The screen is typeset as a **library
card catalog** — the pre-software filing system everyone understands — because
that is literally what the app is: drawers, labels, counts, and cards. The
sloth theme is in the behavior, not the decoration: nothing on this screen
asks the user to do anything. They drop things in; the catalog sorts.

Same visual family as Gazette (Week 2, "the statement"): one type family,
hairline rules, zero radius, zero shadow, one accent. A filing system has no
chat bubbles.

## Colors

- **Paper (#F6F8F4):** the only surface. Daylight-only v1.
- **Ink (#161A17):** titles, labels, snippet text primary.
- **Ink-soft (#5E6660):** verbatim snippet text, timestamps, INBOX drawer.
- **Hairline (#E0E4DC):** every divider. No cards, no shadows.
- **Pine (#0E4F34):** the one accent, reserved for **what the model did while
  you slept**: the "filed overnight" summary count and the per-drawer counts
  of machine-filed snippets. It is the lazy payoff, made visible.
- **Pine-wash (#EAF0EB):** fills the FILE NOW pill only.

## Typography

- **Schibsted Grotesk** throughout, bundled in the APK (same license file as
  Gazette). Tabular figures via `tnum` so drawer counts align.
- `drawer-label`: tracked uppercase (0.14em) — the typed label on a catalog
  drawer front. All eight categories + UNFILED + INBOX render in this style.
- `snippet-title`: what the model wrote; `snippet-text`: the verbatim
  evidence underneath. Evidence, not paraphrase — same rule as Gazette.

## Layout

Single screen, top to bottom:

1. **Wordmark + status line.** "SIFT" left; status right
   ("7 kept · 0 filed" while the queue waits, "7 filed" once it drains).
   Hairline below, then the summary double rule (the catalog's header
   signature, echoing Gazette's accounting double rule).
2. **UNFILED section** (only while items await the night): gray label,
   verbatim rows, and a single pulsing **SIFTING** row while the worker
   runs (the READING shimmer equivalent, same rhythm).
3. **Drawer sections.** Fixed taxonomy order: ADDRESS, CODE, CONTACT,
   EVENT, IDEA, LINK, LIST, RECIPE — every drawer shown, even at count 0
   (a catalog with all its drawers visible is the point; empty ones just
   show their label). Drawer label left, tabular count right, hairline
   below the header. Snippets inside, newest first.
4. **INBOX last**, in ink-soft: the fail-safe drawer. Visually quieter —
   it is the drawer for things even the model wouldn't guess.

## Elevation & Depth

None. Flat paper, hairline rules, and typographic hierarchy only.

## Shapes

Zero border radius everywhere. A catalog card is a rectangle.

## Components

- **Drawer section:** label + count header, hairline, rows. No chevrons,
  no collapse — the whole taxonomy fits on one screen; scrolling is fine.
- **Snippet row:** model title (ink) over verbatim text (ink-soft).
  Machine-filed-this-morning rows keep a 2dp pine tick on the left edge
  for the first morning only — after that they age into ink.
- **FILE NOW:** a single tracked-uppercase text button in the top section
  while UNFILED items exist (pine-wash pill). It is the impatient path,
  deliberately small: the overnight job is the real product.
- **Setup state (model missing):** GET THE FILE / I HAVE THE FILE pair,
  same contract as Gazette, typeset as an instruction card on paper.

## Do's and Don'ts

- Do keep pine for model-did-this semantics only. Never for decoration.
- Don't add icons, avatars, shadows, or radius. Cards are rectangles;
  drawers are labels.
- Don't paraphrase snippets — verbatim text under the title, always.
- Don't animate anything except the SIFTING pulse and the drawer count
  flipping when a snippet lands. The catalog is quiet.
- Do show empty drawers; don't show empty-state placeholders inside them.
