// Sift spike v2: few-shot prompt (one example per label, alternating chat turns).
// Fixes v1 failures: label-echo ("BELL", "WiFi"), prompt-parroting ("THE label"),
// format non-compliance. num_predict 12 (v1's 5 truncated verbose replies).
// Usage: node scripts/sift_test_v2.js [model] [votes]
const fs = require('fs');
const path = require('path');

const MODEL = process.argv[2] || 'gemma3:1b';
const VOTES = Number(process.argv[3] || 3);
const corpusPath = path.join(__dirname, '..', 'corpus', 'snippets_v1.jsonl');
const outPath = path.join(__dirname, '..', `results_sift_v2_${MODEL.replace(/[:.]/g, '_')}.jsonl`);
const entries = fs.readFileSync(corpusPath, 'utf8').trim().split(/\r?\n/).map(JSON.parse);

const SYSTEM = `You are the filing brain of Sift, a phone app that saves short notes from anywhere and files them into drawers. For every note you reply with exactly one word: the drawer label. Never any other word, never an explanation.`;

const FEWSHOT = [
  ['Grandma\'s new flat is 12B, Lantern Court, third floor', 'ADDRESS'],
  ['Netflix password: p0pcorn!Tin', 'CODE'],
  ['Text me at +1 415 555 0134 once you land', 'CONTACT'],
  ['Doctor appointment Tuesday 9:15am, bring the old reports', 'EVENT'],
  ['Gift idea: the green enamel kettle she kept looking at', 'IDEA'],
  ['Read later: example.com/why-bridges-hum', 'LINK'],
  ['Buy: candles, cardamom, two coconuts, red thread', 'LIST'],
  ['Kheer: slow-cook milk 40 min, handful of rice, cardamom, serve cold', 'RECIPE'],
  ['Haha nice one, see you at the thing', 'INBOX'],
];

const TITLE_SYSTEM = `You write very short titles for saved notes. Reply with a title of 3 to 6 words, plain text, no quotes, no trailing punctuation. Reply with the title only.`;

function categoryMessages(text) {
  const msgs = [{ role: 'system', content: SYSTEM }];
  for (const [note, label] of FEWSHOT) {
    msgs.push({ role: 'user', content: `Note: "${note}"\nLabel:` });
    msgs.push({ role: 'assistant', content: label });
  }
  msgs.push({ role: 'user', content: `Note: "${text}"\nLabel:` });
  return msgs;
}

async function gen(messages, numPredict, seed) {
  const res = await fetch('http://localhost:11434/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: MODEL,
      stream: false,
      messages,
      options: { temperature: 0.4, top_p: 0.9, num_predict: numPredict, num_gpu: 0, seed },
    }),
  });
  const j = await res.json();
  return (j.message?.content ?? '').trim();
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
      const raw = await gen(categoryMessages(e.text), 12, seed);
      rawVotes.push(raw);
      const n = normalizeLabel(raw);
      if (!n) malformed++;
      votes.push(n || 'OTHER');
    }
    const predicted = majority(votes);

    const rawTitle = await gen(
      [{ role: 'system', content: TITLE_SYSTEM }, { role: 'user', content: `Note: "${e.text}"\nTitle:` }],
      16, Math.floor(Math.random() * 2 ** 31)
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

  console.log('\n==== SUMMARY v2 (few-shot) ====');
  console.log(`model=${MODEL} votes=${VOTES} corpus=${entries.length} (32 clear + 8 ambiguous), ALL SNIPPETS INVENTED`);
  console.log(`clear:   exact ${cExact}/32, acceptable ${cAccept}/32`);
  console.log(`ambiguous: exact ${aExact}/8, acceptable ${aAccept}/8`);
  console.log(`fail-safe INBOX predictions: ${inboxCount}/${entries.length}  | malformed raw outputs: ${malformed}/${entries.length * VOTES}`);
  console.log(`titles kept (non-degenerate): ${titled}/${entries.length}`);
  console.log(`total ${t.toFixed(1)}s = ${(t / entries.length).toFixed(1)}s/entry`);
  console.log(`results: ${outPath}`);
})();
