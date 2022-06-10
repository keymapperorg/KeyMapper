package io.github.sds100.keymapper.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.FakeResourceProvider
import io.github.sds100.keymapper.util.ui.PopupUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 29/04/2022.
 */
@ExperimentalCoroutinesApi
class HomeMenuViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher)
    private lateinit var fakeResourceProvider: FakeResourceProvider
    private lateinit var viewModel: HomeMenuViewModel

    @Before
    fun setUp() {
        fakeResourceProvider = FakeResourceProvider()
        viewModel = HomeMenuViewModel(
            testCoroutineScope,
            alertsUseCase = mock(),
            pauseMappings = mock(),
            showImePicker = mock(),
            fakeResourceProvider
        )
    }

    @Test
    fun onCreateDocumentActivityNotFound() = runTest(testDispatcher) {
        //given
        fakeResourceProvider.stringResourceMap[R.string.dialog_message_no_app_found_to_create_file] = "message"
        fakeResourceProvider.stringResourceMap[R.string.pos_ok] = "ok"

        //when
        viewModel.onCreateBackupFileActivityNotFound()

        //then
        withTimeout(1000) {
            val popupEvent = viewModel.showPopup.first()
            assertThat(popupEvent.ui, `is`(PopupUi.Dialog(message = "message", positiveButtonText = "ok")))
        }
    }

    @Test
    fun onGetContentActivityNotFound() = runTest(testDispatcher) {
        //given
        fakeResourceProvider.stringResourceMap[R.string.dialog_message_no_app_found_to_choose_a_file] = "message"
        fakeResourceProvider.stringResourceMap[R.string.pos_ok] = "ok"

        //when
        viewModel.onChooseRestoreFileActivityNotFound()

        //then
        withTimeout(1000) {
            val popupEvent = viewModel.showPopup.first()
            assertThat(popupEvent.ui, `is`(PopupUi.Dialog(message = "message", positiveButtonText = "ok")))
        }
    }
}