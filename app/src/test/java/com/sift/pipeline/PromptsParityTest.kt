package com.sift.pipeline

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Freezes the on-device prompt text so accidental edits cannot silently
// diverge from the prompt the calibration numbers were measured with.
class PromptsParityTest {

    @Test
    fun categorizePromptCarriesFewShotExamples() {
        val prompt = Prompts.categorize("Flat 4B, Rosewood Apartments")
        for (label in Labels.TAXONOMY + Labels.INBOX) {
            assertTrue("missing example for $label", prompt.contains("-> $label"))
        }
        assertTrue(prompt.contains("Note: \"Flat 4B, Rosewood Apartments\""))
        // The closed-set reminder must sit adjacent to the answer slot
        // (the Gazette lesson: without it the 1B invents labels).
        assertTrue(
            prompt.trimEnd().endsWith(
                "Answer with one word (ADDRESS, CODE, CONTACT, EVENT, IDEA, LINK, LIST, RECIPE, INBOX):"
            )
        )
    }

    @Test
    fun titlePromptAsksForShortPlainTitle() {
        val prompt = Prompts.title("Milk, eggs")
        assertTrue(prompt.contains("3 to 6 words"))
        assertTrue(prompt.trimEnd().endsWith("Title:"))
    }

    @Test
    fun longSnippetsTruncatedBeforeTheModel() {
        // The pipeline contract: >2000 chars goes to the model truncated,
        // while the stored text stays verbatim.
        val long = "a".repeat(2500)
        val promptSeen = StringBuilder()
        val engine = RecordingEngine { p ->
            promptSeen.append(p)
            "RECIPE"
        }
        runBlocking { SiftPipeline(engine).process(long) }
        assertTrue(promptSeen.length < 3000)
        assertTrue(promptSeen.contains("a".repeat(2000)))
        assertFalse(promptSeen.contains("a".repeat(2001)))
    }

    private class RecordingEngine(val onGenerate: (String) -> String) : LlmEngine {
        override suspend fun generate(prompt: String, maxNewTokens: Int, temperature: Double) =
            onGenerate(prompt)
    }
}
