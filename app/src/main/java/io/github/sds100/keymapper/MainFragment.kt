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
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.navigation.NavResult
import io.github.sds100.keymapper.base.utils.navigation.NavigateEvent
import io.github.sds100.keymapper.base.utils.navigation.NavigationViewModel
import io.github.sds100.keymapper.base.utils.navigation.setupNavigation
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var navigationProvider: NavigationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationProvider.setupNavigation(this)
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
                    val navEvent: NavigateEvent? by navigationProvider.navigate.collectAsStateWithLifecycle(
                        null,
                    )
                    val navResult: NavResult? by navigationProvider.onNavResult.collectAsStateWithLifecycle(
                        null,
                    )

                    LaunchedEffect(navEvent) {
                        val navEvent = navEvent
                        if (navEvent == null) {
                            return@LaunchedEffect
                        }

                        if (!navEvent.destination.isCompose) {
                            return@LaunchedEffect
                        }

                        navController.navigate(navEvent.destination)
                        navigationProvider.handledEvent()
                    }

                    LaunchedEffect(navResult) {
                        val navResult = navResult

                        if (navResult == null) {
                            return@LaunchedEffect
                        }

                        navController.navigateUp()
                        navigationProvider.handledResult()
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

        // TODO
//        homeViewModel.showPopups(this, view)
//        homeViewModel.keyMapListViewModel.showPopups(this, view)
        // TODO
//        homeViewModel.listFloatingLayoutsViewModel.showPopups(this, view)
    }
}
