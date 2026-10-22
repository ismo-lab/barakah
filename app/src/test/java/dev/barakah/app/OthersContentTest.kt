package dev.barakah.app

import dev.barakah.app.data.OthersContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OthersContentTest {

    @Test
    fun coversEveryTranslatedLanguage() {
        val content = OthersContent.load(RuntimeEnvironment.getApplication())
        val languages = listOf("id", "de", "ur", "bn", "tr", "fa", "ha", "ms", "sw", "ru", "ckb")
        for (language in languages) {
            for (id in 1..11) {
                assertTrue(
                    "$language missing occasion $id",
                    content.occasionName(id, language, "fallback") != "fallback" &&
                        content.occasionDescription(id, language, "fallback") != "fallback"
                )
            }
        }
        assertEquals("unsupported language fallback", "fallback", content.occasionName(1, "xx", "fallback"))
    }
}
