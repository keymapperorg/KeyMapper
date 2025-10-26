package io.github.sds100.keymapper.base.system.intents

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.FakeResourceProvider
import io.github.sds100.keymapper.base.utils.ui.MultiChoiceItem
import io.github.sds100.keymapper.base.utils.ui.ShowDialogEvent
import io.github.sds100.keymapper.base.utils.ui.onUserResponse
import io.github.sds100.keymapper.common.utils.firstBlocking
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
        viewModel =
            ConfigIntentViewModel(
                fakeResourceProvider,
                dialogProvider = DialogProviderImpl(),
            )
    }

    @Test
    fun showFlagsDialog_whenNoFlags_checkNoFlags() {
        viewModel.showFlagsDialog()
        val popupEvent: ShowDialogEvent = viewModel.showDialog.firstBlocking()
        val multipleChoiceDialog = popupEvent.ui as DialogModel.MultiChoice<*>

        assertThat(multipleChoiceDialog.items.none { it.isChecked }, `is`(true))
    }

    @Test
    fun showFlagsDialog_whenFlags_checkFlags() {
        viewModel.showFlagsDialog()
        val addFlagPopupEvent: ShowDialogEvent = viewModel.showDialog.firstBlocking()
        viewModel.onUserResponse(addFlagPopupEvent.key, listOf(Intent.FLAG_ACTIVITY_NEW_TASK))

        viewModel.showFlagsDialog()
        val popupEvent: ShowDialogEvent = viewModel.showDialog.firstBlocking()
        val multipleChoiceDialog = popupEvent.ui as DialogModel.MultiChoice<*>
        val expectedCheckedItem =
            MultiChoiceItem(Intent.FLAG_ACTIVITY_NEW_TASK, "FLAG_ACTIVITY_NEW_TASK", true)

        assertThat(multipleChoiceDialog.items, hasItem(expectedCheckedItem))
    }
}
