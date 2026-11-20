package io.github.sds100.keymapper.base.sorting

import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapNameComparator
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class KeyMapNameComparatorTest {
    private val keyMaps = listOf("", "banana", " ", "Cherry", "apple").map { KeyMap(name = it) }

    @Test
    fun `sort alphabetically ignoring case with unnamed key maps last`() {
        val names = keyMaps.sortedWith(KeyMapNameComparator()).map { it.name }

        assertThat(names, `is`(listOf("apple", "banana", "Cherry", "", " ")))
    }

    @Test
    fun `unnamed key maps are still last when reversed`() {
        val names = keyMaps.sortedWith(KeyMapNameComparator(reverse = true)).map { it.name }

        assertThat(names, `is`(listOf("Cherry", "banana", "apple", "", " ")))
    }
}
