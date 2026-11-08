package app.vireo.data.xtream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamTitleNormalizerTest {

    @Test
    fun `normalizes diacritics punctuation and bracketed year`() {
        assertEquals(
            "amelie",
            XtreamTitleNormalizer.normalize("Amélie (2001)"),
        )
    }

    @Test
    fun `extracts year from parentheses and square brackets`() {
        assertEquals(2021, XtreamTitleNormalizer.extractYear("Dune (2021)"))
        assertEquals(2019, XtreamTitleNormalizer.extractYear("Joker [2019]"))
    }

    @Test
    fun `does not assume a trailing title number is a release year`() {
        assertNull(XtreamTitleNormalizer.extractYear("Blade Runner 2049"))
        assertEquals(
            "blade runner 2049",
            XtreamTitleNormalizer.normalize("Blade Runner 2049"),
        )
    }

    @Test
    fun `normalizes ampersands and repeated whitespace`() {
        assertEquals(
            "rock and roll",
            XtreamTitleNormalizer.normalize("Rock  &  Roll"),
        )
    }

    @Test
    fun `empty punctuation does not produce a searchable title`() {
        assertEquals("", XtreamTitleNormalizer.normalize(" !!! "))
    }


    @Test
    fun `removes language quality and bracket metadata`() {
        assertEquals(
            "dune",
            XtreamTitleNormalizer.normalize("EN | Dune (2021) [1080p]"),
        )
        assertEquals(
            "dune",
            XtreamTitleNormalizer.normalize("PL | Dune 4K"),
        )
    }

    @Test
    fun `normalizes provider release tags without losing title words`() {
        assertEquals(
            "the lord of the rings",
            XtreamTitleNormalizer.normalize("The Lord of the Rings [MULTI] UHD"),
        )
    }

    @Test
    fun `extracts year from parenthesized and bracketed metadata`() {
        assertEquals(2021, XtreamTitleNormalizer.extractYear("Dune (2021)"))
        assertEquals(2019, XtreamTitleNormalizer.extractYear("Joker [2019]"))
    }

}
