package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.system.inputmethod.AndroidInputMethodAdapter.Companion.getSubtypeHashCodeFromHistory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

class SubtypeHistoryTest {
    private val keyMapperIme = "io.github.sds100.keymapper/.ime"
    private val gboard = "com.google.latin/.LatinIME"
    private val history = "$keyMapperIme;-921088104:$gboard;1891618174"

    @Test
    fun `find the subtype of an input method that is not first in the history`() {
        assertThat(getSubtypeHashCodeFromHistory(history, gboard), `is`(1891618174))
    }

    @Test
    fun `find negative subtype hash codes`() {
        assertThat(getSubtypeHashCodeFromHistory(history, keyMapperIme), `is`(-921088104))
    }

    @Test
    fun `return null if the input method is not in the history`() {
        assertThat(getSubtypeHashCodeFromHistory(history, "other/.Ime"), nullValue())
        assertThat(getSubtypeHashCodeFromHistory(null, "other/.Ime"), nullValue())
    }

    @Test
    fun `return null if the entry has no subtype`() {
        assertThat(getSubtypeHashCodeFromHistory("other/.Ime", "other/.Ime"), nullValue())
    }
}
