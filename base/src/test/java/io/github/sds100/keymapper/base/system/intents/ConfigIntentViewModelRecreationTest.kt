package io.github.sds100.keymapper.base.system.intents

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.FakeResourceProvider
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.system.intents.IntentTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for issue #2160. The fragment applies the initial argument by calling
 * [ConfigIntentViewModel.loadResult] from onCreate, which runs again when the screen is recreated
 * (for example on a configuration change) while the ViewModel survives. Loading must be applied
 * only once so that a recreation does not discard the user's edits with the original value.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ConfigIntentViewModelRecreationTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ConfigIntentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ConfigIntentViewModel(FakeResourceProvider(), DialogProviderImpl())
    }

    @Test
    fun loadResult_recreationAfterChangingActivity_keepsEditedActivity() = runTest(testDispatcher) {
        val original = ConfigIntentResult(
            uri = "#Intent;package=com.example.a;component=com.example.a/.MainActivity;end",
            target = IntentTarget.ACTIVITY,
            description = "Open App",
            extras = emptyList(),
        )

        // Edit an existing intent action.
        viewModel.loadResult(original)

        // The user picks a different activity.
        viewModel.setActivity(ActivityInfo("com.example.b.SecondActivity", "com.example.b"))

        // The screen is recreated, so onCreate applies the original argument again.
        viewModel.loadResult(original)

        assertThat(viewModel.targetPackage.value, `is`("com.example.b"))
        assertThat(viewModel.targetClass.value, `is`("com.example.b.SecondActivity"))

        // The saved intent uri must contain the edited activity, not the original one.
        val result = collectDoneResult()
        val component = Intent.parseUri(result.uri, 0).component
        assertThat(component?.packageName, `is`("com.example.b"))
        assertThat(component?.className, `is`("com.example.b.SecondActivity"))
    }

    private suspend fun collectDoneResult(): ConfigIntentResult {
        var result: ConfigIntentResult? = null
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            result = viewModel.returnResult.first()
        }
        viewModel.onDoneClick()
        job.join()
        return result!!
    }
}
