package com.sift.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoteParsingTest {

    // ---- parseLabel ----

    @Test
    fun parseLabel_exact() {
        assertEquals("RECIPE", VoteParsing.parseLabel("RECIPE"))
    }

    @Test
    fun parseLabel_inSentence() {
        assertEquals("ADDRESS", VoteParsing.parseLabel("The answer is address."))
    }

    @Test
    fun parseLabel_lowercase() {
        assertEquals("CODE", VoteParsing.parseLabel("code"))
    }

    @Test
    fun parseLabel_garbageIsNull() {
        assertNull(VoteParsing.parseLabel("BELL"))
        assertNull(VoteParsing.parseLabel("WiFi"))
        assertNull(VoteParsing.parseLabel(""))
    }

    @Test
    fun parseLabel_firstMatchWins() {
        assertEquals("LINK", VoteParsing.parseLabel("LINK not LIST"))
    }

    // ---- majority ----

    @Test
    fun majority_twoOfThreeWins() {
        assertEquals("EVENT", VoteParsing.majority(listOf("EVENT", "EVENT", "IDEA")))
    }

    @Test
    fun majority_disagreementIsInbox() {
        assertEquals("INBOX", VoteParsing.majority(listOf("EVENT", "IDEA", "LIST")))
    }

    @Test
    fun majority_malformedVotesCanNeverWin() {
        // The JS spike could return OTHER here; in-app this must be INBOX.
        assertEquals("INBOX", VoteParsing.majority(listOf(null, null, "IDEA")))
    }

    @Test
    fun majority_allMalformedIsInbox() {
        assertEquals("INBOX", VoteParsing.majority(listOf(null, null, null)))
    }

    @Test
    fun majority_singleVoteMode() {
        assertEquals("CODE", VoteParsing.majority(listOf("CODE")))
        assertEquals("INBOX", VoteParsing.majority(listOf(null)))
    }

    // ---- titles ----

    @Test
    fun title_emptyIsDegenerate() {
        assertTrue(VoteParsing.titleLooksDegenerate("", "some text"))
    }

    @Test
    fun title_tooLongIsDegenerate() {
        assertTrue(VoteParsing.titleLooksDegenerate("x".repeat(61), "some text"))
    }

    @Test
    fun title_notePrefixIsDegenerate() {
        assertTrue(VoteParsing.titleLooksDegenerate("Note: wifi", "some text"))
        assertTrue(VoteParsing.titleLooksDegenerate("note :", "some text"))
    }

    @Test
    fun title_echoesTextIsDegenerate() {
        val text = "WiFi at my place: HomeNet_5G"
        assertTrue(VoteParsing.titleLooksDegenerate(text, text))
    }

    @Test
    fun title_metaWordsAreDegenerate() {
        assertTrue(VoteParsing.titleLooksDegenerate("The category is food", "some text"))
        assertTrue(VoteParsing.titleLooksDegenerate("Label: ADDRESS", "some text"))
    }

    @Test
    fun title_goodTitleSurvives() {
        assertFalse(VoteParsing.titleLooksDegenerate("Door Code: 4471 #", "Door code is 4471 then #"))
    }

    // ---- fallbackTitle ----

    @Test
    fun fallback_firstLine() {
        assertEquals("Line one", VoteParsing.fallbackTitle("Line one\nLine two"))
    }

    @Test
    fun fallback_truncatedAt48() {
        val long = "x".repeat(80)
        val title = VoteParsing.fallbackTitle(long)
        assertEquals(48, title.length)
        assertTrue(title.endsWith("…"))
    }
}
