package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.common.utils.UndoRedo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

class UndoRedoTest {
    @Test
    fun `undo and redo walk through recorded values`() {
        val undoRedo = UndoRedo<Int>()
        undoRedo.reset(1)
        undoRedo.record(2)
        undoRedo.record(3)

        assertThat(undoRedo.state.value.canUndo, `is`(true))
        assertThat(undoRedo.state.value.canRedo, `is`(false))
        assertThat(undoRedo.undo(), `is`(2))
        assertThat(undoRedo.undo(), `is`(1))
        assertThat(undoRedo.undo(), `is`(nullValue()))
        assertThat(undoRedo.state.value.canUndo, `is`(false))
        assertThat(undoRedo.redo(), `is`(2))
        assertThat(undoRedo.redo(), `is`(3))
        assertThat(undoRedo.redo(), `is`(nullValue()))
    }

    @Test
    fun `recording after an undo discards the redo branch`() {
        val undoRedo = UndoRedo<Int>()
        undoRedo.reset(1)
        undoRedo.record(2)
        undoRedo.undo()
        undoRedo.record(3)

        assertThat(undoRedo.state.value.canRedo, `is`(false))
        assertThat(undoRedo.undo(), `is`(1))
    }

    @Test
    fun `reset clears history`() {
        val undoRedo = UndoRedo<Int>()
        undoRedo.reset(1)
        undoRedo.record(2)
        undoRedo.reset(5)

        assertThat(undoRedo.state.value.canUndo, `is`(false))
        assertThat(undoRedo.state.value.canRedo, `is`(false))
    }
}
