package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.ImeSubtypeInfo
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class CycleKeyboardLanguageTest {
    private val gboard = ImeInfo(
        id = "com.google.latin/.LatinIME",
        packageName = "com.google.latin",
        label = "Gboard",
        isEnabled = true,
        isChosen = true,
    )
    private val english = ImeSubtypeInfo(id = 1, languageTag = "en-US")
    private val bulgarian = ImeSubtypeInfo(id = 2, languageTag = "bg")

    private lateinit var fakeImeAdapter: FakeInputMethodAdapter
    private lateinit var imeHelper: KeyMapperImeHelper

    @Before
    fun init() {
        fakeImeAdapter = FakeInputMethodAdapter()
        fakeImeAdapter.inputMethods.value = listOf(gboard)
        fakeImeAdapter.chosenIme.value = gboard
        fakeImeAdapter.enabledSubtypes[gboard.id] = listOf(english, bulgarian)

        imeHelper = KeyMapperImeHelper(mock(), fakeImeAdapter, "io.github.sds100.keymapper")
    }

    @Test
    fun `select the next subtype`() = runTest {
        fakeImeAdapter.selectedSubtypeId = english.id
        imeHelper.cycleKeyboardLanguage()
        assertThat(fakeImeAdapter.selectedSubtypeId, `is`(bulgarian.id))
    }

    @Test
    fun `wrap around to the first subtype after the last`() = runTest {
        fakeImeAdapter.selectedSubtypeId = bulgarian.id
        imeHelper.cycleKeyboardLanguage()
        assertThat(fakeImeAdapter.selectedSubtypeId, `is`(english.id))
    }

    @Test
    fun `select the first subtype if none is selected`() = runTest {
        imeHelper.cycleKeyboardLanguage()
        assertThat(fakeImeAdapter.selectedSubtypeId, `is`(english.id))
    }

    @Test
    fun `fail if there are fewer than 2 subtypes`() = runTest {
        fakeImeAdapter.enabledSubtypes[gboard.id] = listOf(english)
        val result = imeHelper.cycleKeyboardLanguage()
        assertThat(result, `is`(KMError.NotEnoughInputMethodSubtypes))
    }
}
