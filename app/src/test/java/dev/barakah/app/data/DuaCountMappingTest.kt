package dev.barakah.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuaCountMappingTest {
    @Test
    fun `only unique exact normalized Arabic match maps count`() {
        val arabic = listOf("سُبْحَانَ اللَّهِ" to 33, "الْحَمْدُ لِلَّهِ" to 10)
        val lookup = arabic.groupBy { normalize(it.first) }

        assertEquals(33, lookup[normalize("سبحان الله")]?.singleOrNull()?.second)
        assertNull(lookup[normalize("سبحان الله وبحمده")]?.singleOrNull()?.second)
        assertNull((arabic + ("سبحان الله" to 5)).groupBy { normalize(it.first) }
            [normalize("سبحان الله")]?.singleOrNull()?.second)
    }

    private fun normalize(text: String): String = buildString {
        text.forEach { char ->
            if (char !in '\u064B'..'\u065F' && char != '\u0670' && char != '\u0640') {
                val normalized = when (char) {
                    'أ', 'إ', 'آ' -> 'ا'
                    'ى' -> 'ي'
                    'ة' -> 'ه'
                    else -> char
                }
                if (normalized in '\u0621'..'\u064A') append(normalized)
            }
        }
    }
}
