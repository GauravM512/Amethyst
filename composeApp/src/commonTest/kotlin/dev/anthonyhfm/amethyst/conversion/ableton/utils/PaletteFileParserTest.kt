package dev.anthonyhfm.amethyst.conversion.ableton.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals

class PaletteFileParserTest {
    @Test
    fun parsesIndexedRgbLinesInOrder() {
        assertContentEquals(
            arrayOf(Triple(0, 0, 0), Triple(63, 31, 7)),
            PaletteFileParser.parsePaletteFileContent("0, 0 0 0;\n1, 63 31 7;"),
        )
    }

    @Test
    fun skipsHeadersMalformedColorsAndBlankLines() {
        assertContentEquals(
            arrayOf(Triple(1, 2, 3), Triple(4, 5, 6)),
            PaletteFileParser.parsePaletteFileContent(
                "header\n0, 1 2 3;\n1, red 2 3;\n2, 4 5 6;\n3, 1 2\n",
            ),
        )
    }

    @Test
    fun emptyFileProducesEmptyPalette() {
        assertContentEquals(emptyArray<Triple<Int, Int, Int>>(), PaletteFileParser.parsePaletteFileContent(""))
    }
}
