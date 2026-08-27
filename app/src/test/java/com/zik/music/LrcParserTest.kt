package com.zik.music

import com.zik.music.data.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun testStandardLrcParsing() {
        val lrc = """
            [ti:Bohemian Rhapsody]
            [ar:Queen]
            [00:01.50]Is this the real life?
            [00:04.20]Is this just fantasy?
            [00:08.100]Caught in a landslide
            [00:12.00]No escape from reality
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(4, parsed.size)
        assertEquals(1500L, parsed[0].timestampMs)
        assertEquals("Is this the real life?", parsed[0].text)
        assertEquals(4200L, parsed[1].timestampMs)
        assertEquals("Is this just fantasy?", parsed[1].text)
        assertEquals(8100L, parsed[2].timestampMs)
        assertEquals("Caught in a landslide", parsed[2].text)
        assertEquals(12000L, parsed[3].timestampMs)
        assertEquals("No escape from reality", parsed[3].text)
    }

    @Test
    fun testMultiTimestampLine() {
        val lrc = """
            [00:05.00][00:20.00]Chorus line repeated
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(2, parsed.size)
        assertEquals(5000L, parsed[0].timestampMs)
        assertEquals("Chorus line repeated", parsed[0].text)
        assertEquals(20000L, parsed[1].timestampMs)
        assertEquals("Chorus line repeated", parsed[1].text)
    }
}
