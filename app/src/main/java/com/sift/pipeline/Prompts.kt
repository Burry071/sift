package com.sift.pipeline

// On-device rendering of the winning spike prompt (scripts/sift_test_v4.js —
// Gazette's raw single-blob format: grouped examples, closed-set answer
// line). These strings are normative — parity with the desktop spike
// depends on them.
object Prompts {

    private val ANSWER_LINE =
        "Answer with one word (ADDRESS, CODE, CONTACT, EVENT, IDEA, LINK, LIST, RECIPE, INBOX):"

    fun categorize(text: String): String = """You read a short note someone saved on their phone. Decide which drawer of a filing catalog it belongs to.

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

Note: "$text"
$ANSWER_LINE"""

    fun title(text: String): String = """You write very short titles for saved notes. Reply with a title of 3 to 6 words, plain text, no quotes, no trailing punctuation. Reply with the title only.

Note: "$text"
Title:"""
}
