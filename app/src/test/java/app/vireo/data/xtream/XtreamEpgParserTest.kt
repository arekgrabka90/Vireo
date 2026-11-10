package app.vireo.data.xtream

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamEpgParserTest {

    @Test
    fun parsesShortEpgPayloadAndKeepsStreamId() {
        val payload = Json.parseToJsonElement(
            """
            {
              "epg_listings": [
                {
                  "title": "Evening News",
                  "description": "Daily news",
                  "start_timestamp": "1794330000",
                  "stop_timestamp": "1794333600",
                  "category": "News"
                }
              ]
            }
            """.trimIndent()
        )

        val result = XtreamEpgParser.parse("stream-42", payload)

        assertEquals(1, result.size)
        assertEquals("stream-42", result.first().channelId)
        assertEquals("Evening News", result.first().title)
        assertEquals("Daily news", result.first().description)
        assertEquals(1_794_330_000_000L, result.first().startTimeMs)
        assertTrue(result.first().hasValidTimeRange)
    }

    @Test
    fun ignoresEntriesWithInvalidTimeRange() {
        val payload = Json.parseToJsonElement(
            """
            [
              {
                "title": "Broken",
                "start_timestamp": "1000",
                "stop_timestamp": "900"
              },
              {
                "title": "Valid",
                "start_timestamp": "1000",
                "stop_timestamp": "2000"
              }
            ]
            """.trimIndent()
        )

        val result = XtreamEpgParser.parse("stream-1", payload)

        assertEquals(1, result.size)
        assertEquals("Valid", result.first().title)
    }
}
