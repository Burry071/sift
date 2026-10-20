package com.sift.pipeline

object Labels {
    val TAXONOMY = listOf("ADDRESS", "CODE", "CONTACT", "EVENT", "IDEA", "LINK", "LIST", "RECIPE")
    const val INBOX = "INBOX"
}

// Exact, tested semantics — mirrors the JS spike, hardened where the spike
// was loose: malformed votes parse to null and can never win a majority
// (the JS could return OTHER; in-app that situation must mean INBOX).
object VoteParsing {

    private val LABEL_RE = Regex("\\b(ADDRESS|CODE|CONTACT|EVENT|IDEA|LINK|LIST|RECIPE|INBOX)\\b")
    private val NOTE_PREFIX_RE = Regex("^note\\s*:", RegexOption.IGNORE_CASE)
    private val META_WORD_RE = Regex("\\b(label|category|assistant)\\b", RegexOption.IGNORE_CASE)

    fun parseLabel(raw: String): String? =
        LABEL_RE.find(raw.uppercase())?.groupValues?.get(1)

    // Majority is measured against ALL votes cast, so garbage output can
    // only force the fail-safe drawer, never a wrong drawer.
    fun majority(votes: List<String?>): String {
        val counts = votes.filterNotNull().groupingBy { it }.eachCount()
        val best = counts.maxByOrNull { it.value } ?: return Labels.INBOX
        return if (best.value >= (votes.size + 1) / 2) best.key else Labels.INBOX
    }

    fun titleLooksDegenerate(title: String, text: String): Boolean {
        if (title.isEmpty()) return true
        if (title.length > 60) return true
        if (NOTE_PREFIX_RE.containsMatchIn(title)) return true
        if (title.lowercase() == text.lowercase().take(title.length)) return true
        if (META_WORD_RE.containsMatchIn(title)) return true
        return false
    }

    fun fallbackTitle(text: String): String {
        val firstLine = text.trim().lineSequence().firstOrNull() ?: ""
        return if (firstLine.length <= 48) firstLine else firstLine.take(47) + "…"
    }
}

class SiftPipeline(
    private val engine: LlmEngine,
    private val votes: Int = 3,
    private val temperature: Double = 0.4,
) {

    data class Filed(val category: String, val title: String, val titleIsFallback: Boolean)

    // Throws on engine failure so the caller can leave the snippet UNFILED
    // and retry next night — a kept snippet must never be silently lost.
    suspend fun process(text: String): Filed {
        val modelText = if (text.length > 2000) text.take(2000) else text
        val parsed = mutableListOf<String?>()
        repeat(votes) {
            parsed += VoteParsing.parseLabel(engine.generate(Prompts.categorize(modelText), 12, temperature))
        }
        val category = VoteParsing.majority(parsed)

        val rawTitle = try {
            engine.generate(Prompts.title(modelText), 16, temperature).trim()
        } catch (e: Exception) {
            // A title failure must not lose the category work already done.
            return Filed(category, VoteParsing.fallbackTitle(text), true)
        }
        val isFallback = VoteParsing.titleLooksDegenerate(rawTitle, text)
        return Filed(category, if (isFallback) VoteParsing.fallbackTitle(text) else rawTitle, isFallback)
    }
}
