package io.github.sds100.keymapper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.navigation.NavResult
import io.github.sds100.keymapper.base.utils.navigation.NavigateEvent
import io.github.sds100.keymapper.base.utils.navigation.NavigationProviderImpl
import io.github.sds100.keymapper.base.utils.navigation.setupFragmentNavigation
import io.github.sds100.keymapper.base.utils.ui.PopupViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.showPopups
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var navigationProvider: NavigationProviderImpl

    @Inject
    lateinit var popupViewModel: PopupViewModelImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationProvider.setupFragmentNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentComposeBinding.inflate(inflater, container, false).apply {
            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val navController = rememberNavController()
                    val navEvent: NavigateEvent? by navigationProvider.onNavigate
                        .collectAsStateWithLifecycle(null)

                    val returnResult: String? by navigationProvider.onReturnResult
                        .collectAsStateWithLifecycle(null)

                    val currentEntry by navController.currentBackStackEntryAsState()

                    val popBackStack by navigationProvider.popBackStack.collectAsStateWithLifecycle(
                        null,
                    )

                    LaunchedEffect(key1 = popBackStack) {
                        popBackStack ?: return@LaunchedEffect

                        navController.navigateUp()
                        navigationProvider.handledPop()
                    }

                    LaunchedEffect(returnResult) {
                        val result = returnResult ?: return@LaunchedEffect

                        // Set the result in previous screen.
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("result", result)

                        navController.navigateUp()
                        navigationProvider.handledReturnResult()
                    }

                    LaunchedEffect(currentEntry) {
                        val currentEntry = currentEntry ?: return@LaunchedEffect

                        val requestKey =
                            currentEntry.savedStateHandle.remove<String>("request_key")

                        // If the current screen has a result then handle it.
                        val data = currentEntry.savedStateHandle.remove<String?>("result")

                        if (requestKey != null && data != null) {
                            navigationProvider.onNavResult(NavResult(requestKey, data))
                            navigationProvider.handledNavResult()
                        }
                    }

                    LaunchedEffect(navEvent) {
                        val navEvent = navEvent ?: return@LaunchedEffect

                        if (!navEvent.destination.isCompose) {
                            return@LaunchedEffect
                        }

                        // Store the request key before navigating.
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("request_key", navEvent.key)

                        navController.navigate(navEvent.destination)

                        navigationProvider.handledNavigateRequest()
                    }

                    KeyMapperTheme {
                        MainNavHost(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                        .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                ),
                            navController = navController,
                            finishActivity = requireActivity()::finish,
                        )
                    }
                }
            }
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        popupViewModel.showPopups(this, view)
    }
}
