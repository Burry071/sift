// Sift spike v4: Gazette's EXACT winning format — /api/generate, one raw
// prompt blob (no system message, no alternating turns), examples grouped
// per label, closed-set answer line. v1-v3 all used /api/chat and failed
// (label-echo / invented labels / wrong-but-in-set picks).
// Usage: node scripts/sift_test_v4.js [model] [votes]
const fs = require('fs');
const path = require('path');

const MODEL = process.argv[2] || 'gemma3:1b';
const VOTES = Number(process.argv[3] || 3);
const corpusPath = path.join(__dirname, '..', 'corpus', 'snippets_v1.jsonl');
const outPath = path.join(__dirname, '..', `results_sift_v4_${MODEL.replace(/[:.]/g, '_')}.jsonl`);
const entries = fs.readFileSync(corpusPath, 'utf8').trim().split(/\r?\n/).map(JSON.parse);

function categorizePrompt(text) {
  return `You read a short note someone saved on their phone. Decide which drawer of a filing catalog it belongs to.

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

Note: "${text}"

Answer with one word (ADDRESS, CODE, CONTACT, EVENT, IDEA, LINK, LIST, RECIPE, INBOX):`;
}

const TITLE_PROMPT_HEAD = `You write very short titles for saved notes. Reply with a title of 3 to 6 words, plain text, no quotes, no trailing punctuation. Reply with the title only.

Note: "`;

async function generate(prompt, predict, seed) {
  const res = await fetch('http://localhost:11434/api/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: MODEL,
      prompt,
      stream: false,
      options: { temperature: 0.4, top_p: 0.9, num_predict: predict, num_gpu: 0, seed },
    }),
  });
  const j = await res.json();
  return (j.response || '').trim();
}

function normalizeLabel(raw) {
  const m = raw.toUpperCase().match(/\b(ADDRESS|CODE|CONTACT|EVENT|IDEA|LINK|LIST|RECIPE|INBOX)\b/);
  return m ? m[1] : null;
}

function majority(votes) {
  const counts = {};
  for (const v of votes) counts[v] = (counts[v] || 0) + 1;
  let best = null, bestN = 0;
  for (const [k, n] of Object.entries(counts)) if (n > bestN) { best = k; bestN = n; }
  return bestN >= 2 ? best : 'INBOX';
}

function titleLooksDegenerate(title, text) {
  if (!title) return true;
  if (title.length > 60) return true;
  if (/^note\s*:/i.test(title)) return true;
  if (title.toLowerCase() === text.toLowerCase().slice(0, title.length)) return true;
  if (/\b(label|category|assistant)\b/i.test(title)) return true;
  return false;
}

(async () => {
  const t0 = Date.now();
  const out = fs.createWriteStream(outPath);
  const clearRows = [], ambigRows = [];
  let malformed = 0;

  for (const e of entries) {
    const te = Date.now();
    const rawVotes = [];
    const votes = [];
    for (let i = 0; i < VOTES; i++) {
      const seed = Math.floor(Math.random() * 2 ** 31);
      const raw = await generate(categorizePrompt(e.text), 5, seed);
      rawVotes.push(raw);
      const n = normalizeLabel(raw);
      if (!n) malformed++;
      votes.push(n || 'OTHER');
    }
    const predicted = majority(votes);

    const rawTitle = await generate(
      `${TITLE_PROMPT_HEAD}${e.text}"\nTitle:`, 16, Math.floor(Math.random() * 2 ** 31)
    );
    const title = titleLooksDegenerate(rawTitle, e.text) ? null : rawTitle;

    const exact = predicted === e.cat;
    const acceptable = e.accept.includes(predicted);
    const row = { id: e.id, expect: e.cat, accept: e.accept, votes: rawVotes, predicted, exact, acceptable, title, ms: Date.now() - te };
    (e.accept.length > 1 ? ambigRows : clearRows).push(row);
    out.write(JSON.stringify(row) + '\n');
    console.log(`[${e.id}/${entries.length}] expect=${e.cat} pred=${predicted} ${exact ? 'OK' : acceptable ? 'ok-set' : 'MISS'} (${row.ms}ms)${title ? ' title="' + title + '"' : ''}`);
  }
  out.end();

  const t = (Date.now() - t0) / 1000;
  const cExact = clearRows.filter(r => r.exact).length;
  const cAccept = clearRows.filter(r => r.acceptable).length;
  const aExact = ambigRows.filter(r => r.exact).length;
  const aAccept = ambigRows.filter(r => r.acceptable).length;
  const inboxCount = [...clearRows, ...ambigRows].filter(r => r.predicted === 'INBOX').length;
  const titled = [...clearRows, ...ambigRows].filter(r => r.title).length;

  console.log('\n==== SUMMARY v4 (Gazette format: raw generate, grouped examples) ====');
  console.log(`model=${MODEL} votes=${VOTES} corpus=${entries.length} (32 clear + 8 ambiguous), ALL SNIPPETS INVENTED`);
  console.log(`clear:   exact ${cExact}/32, acceptable ${cAccept}/32`);
  console.log(`ambiguous: exact ${aExact}/8, acceptable ${aAccept}/8`);
  console.log(`fail-safe INBOX predictions: ${inboxCount}/${entries.length}  | malformed raw outputs: ${malformed}/${entries.length * VOTES}`);
  console.log(`titles kept (non-degenerate): ${titled}/${entries.length}`);
  console.log(`total ${t.toFixed(1)}s = ${(t / entries.length).toFixed(1)}s/entry`);
  console.log(`results: ${outPath}`);
})();
