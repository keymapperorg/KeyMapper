package io.github.sds100.keymapper.system.intents

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.ui.intents.ConfigIntentViewModel
import io.github.sds100.keymapper.common.util.firstBlocking
import io.github.sds100.keymapper.base.util.ui.FakeResourceProvider
import io.github.sds100.keymapper.base.util.ui.MultiChoiceItem
import io.github.sds100.keymapper.base.util.ui.PopupUi
import io.github.sds100.keymapper.base.util.ui.ShowPopupEvent
import io.github.sds100.keymapper.base.util.ui.onUserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
internal class ConfigIntentViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeResourceProvider: FakeResourceProvider
    private lateinit var viewModel: ConfigIntentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeResourceProvider = FakeResourceProvider()
        viewModel = ConfigIntentViewModel(fakeResourceProvider)
    }

    @Test
    fun showFlagsDialog_whenNoFlags_checkNoFlags() {
        viewModel.showFlagsDialog()
        val popupEvent: ShowPopupEvent = viewModel.showPopup.firstBlocking()
        val multipleChoiceDialog = popupEvent.ui as PopupUi.MultiChoice<*>

        assertThat(multipleChoiceDialog.items.none { it.isChecked }, `is`(true))
    }

    @Test
    fun showFlagsDialog_whenFlags_checkFlags() {
        viewModel.showFlagsDialog()
        val addFlagPopupEvent: ShowPopupEvent = viewModel.showPopup.firstBlocking()
        viewModel.onUserResponse(addFlagPopupEvent.key, listOf(Intent.FLAG_ACTIVITY_NEW_TASK))

        viewModel.showFlagsDialog()
        val popupEvent: ShowPopupEvent = viewModel.showPopup.firstBlocking()
        val multipleChoiceDialog = popupEvent.ui as PopupUi.MultiChoice<*>
        val expectedCheckedItem =
            MultiChoiceItem(Intent.FLAG_ACTIVITY_NEW_TASK, "FLAG_ACTIVITY_NEW_TASK", true)

        assertThat(multipleChoiceDialog.items, hasItem(expectedCheckedItem))
    }
}
