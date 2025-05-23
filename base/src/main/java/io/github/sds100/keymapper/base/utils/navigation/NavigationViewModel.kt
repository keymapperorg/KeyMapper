package io.github.sds100.keymapper.base.utils.navigation

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.base.NavBaseAppDirections
import io.github.sds100.keymapper.base.actions.keyevent.ChooseKeyCodeFragment
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventActionFragment
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileFragment
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.actions.tapscreen.PickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.system.apps.ChooseActivityFragment
import io.github.sds100.keymapper.base.system.apps.ChooseAppFragment
import io.github.sds100.keymapper.base.system.apps.ChooseAppShortcutFragment
import io.github.sds100.keymapper.base.system.bluetooth.ChooseBluetoothDeviceFragment
import io.github.sds100.keymapper.base.system.intents.ConfigIntentFragment
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// TODO rename
@Singleton
class NavigationViewModelImpl @Inject constructor() : NavigationViewModel {
    private val _onNavResult = MutableStateFlow<NavResult?>(null)
    override val onNavResult = _onNavResult.asStateFlow()

    private val _navigate = MutableStateFlow<NavigateEvent?>(null)
    override val navigate = _navigate.asStateFlow()

    private val _onReturnResult = MutableStateFlow<String?>(null)
    override val onReturnResult: StateFlow<String?> = _onReturnResult.asStateFlow()

    private val _popBackStack = MutableStateFlow<Unit?>(null)
    val popBackStack: StateFlow<Unit?> = _popBackStack.asStateFlow()

    fun handledPop() {
        _popBackStack.update { null }
    }

    override fun handledReturnResult() {
        _onReturnResult.update { null }
    }

    override fun handledEvent() {
        _navigate.update { null }
    }

    override fun handledNavResult() {
        _onNavResult.update { null }
    }

    override suspend fun navigate(event: NavigateEvent) {
        // wait for the view to collect so navigating can happen
        _navigate.subscriptionCount.first { it > 0 }

        _navigate.emit(event)
    }

    override fun onNavResult(result: NavResult) {
        runBlocking { _onNavResult.emit(result) }
    }

    override suspend fun popBackStack() {
        _popBackStack.value = Unit
    }

    /**
     * @param data The data in String or JSON format to return.
     */
    override suspend fun popBackStackWithResult(data: String) {
        _onReturnResult.subscriptionCount.first { it > 0 }
        _onReturnResult.emit(data)
    }
}

interface NavigationViewModel {
    val navigate: StateFlow<NavigateEvent?>
    suspend fun navigate(event: NavigateEvent)
    fun handledEvent()

    val onNavResult: StateFlow<NavResult?>
    fun onNavResult(result: NavResult)
    fun handledNavResult()

    val onReturnResult: StateFlow<String?>
    fun handledReturnResult()

    suspend fun popBackStackWithResult(data: String)
    suspend fun popBackStack()
}

suspend inline fun <reified R> NavigationViewModel.navigate(
    key: String,
    destination: NavDestination<R>,
): R? {
    navigate(NavigateEvent(key, destination))

    /*
    This ensures only one job for a dialog is active at once by cancelling previous jobs when a new
    dialog is shown with the same key.

    Must drop the first value because it came from our call to navigate.
     */
    return merge(
        navigate.drop(1).filterNotNull().dropWhile { it.key != key }.map { null },
        onNavResult.filterNotNull()
            .dropWhile { it.key != key }
            .map { result -> result.data?.let { Json.decodeFromString<R>(it) } },
    ).first()
}

/**
 * Must call in fragment's onCreate
 */
fun NavigationViewModel.setupNavigation(fragment: Fragment) {
    val navigationSavedStateKey = "navigation:${this.javaClass.name}"

    val pendingResultsKeysExtra = "pending_results_keys"
    val pendingResultsDestinationsExtra = "pending_results_destinations"

    /**
     * Maps request keys to their destination.
     */
    val pendingResults = mutableMapOf<String, String>()

    fragment.savedStateRegistry.registerSavedStateProvider(navigationSavedStateKey) {
        bundleOf(
            pendingResultsKeysExtra to pendingResults.keys.toTypedArray(),
            pendingResultsDestinationsExtra to pendingResults.values.toTypedArray(),
        )
    }

    fragment.savedStateRegistry.consumeRestoredStateForKey(navigationSavedStateKey)
        ?.let { bundle ->
            val oldPendingResultsKeys: Array<String> =
                bundle.getStringArray(pendingResultsKeysExtra)!!

            val oldPendingResultsDestinations: Array<String> =
                bundle.getStringArray(pendingResultsDestinationsExtra)!!

            oldPendingResultsKeys.forEachIndexed { i, requestKey ->
                val destination = oldPendingResultsDestinations[i]

                pendingResults[requestKey] = destination

                fragment.setFragmentResultListener(requestKey) { _, bundle ->
                    sendNavResultFromBundle(requestKey, destination, bundle)
                }
            }
        }

    navigate
        .filterNotNull()
        .filter { !it.destination.isCompose }
        .onEach { event ->
            val (requestKey, destination) = event

            pendingResults[requestKey] = destination.id

            fragment.clearFragmentResultListener(requestKey)

            fragment.setFragmentResultListener(requestKey) { _, bundle ->
                pendingResults.remove(event.key)
                sendNavResultFromBundle(event.key, event.destination.id, bundle)
                handledNavResult()
            }

            val direction = getDirection(destination, requestKey)

            fragment.findNavController().navigate(direction)
            handledEvent()
        }.launchIn(fragment.lifecycleScope)
}

private fun getDirection(destination: NavDestination<*>, requestKey: String): NavDirections {
    return when (destination) {
        is NavDestination.ChooseApp -> NavBaseAppDirections.chooseApp(
            destination.allowHiddenApps,
            requestKey,
        )

        NavDestination.ChooseAppShortcut -> NavBaseAppDirections.chooseAppShortcut(requestKey)
        NavDestination.ChooseKeyCode -> NavBaseAppDirections.chooseKeyCode(requestKey)
        is NavDestination.ConfigKeyEventAction -> {
            val json = destination.action?.let {
                Json.encodeToString(it)
            }

            NavBaseAppDirections.configKeyEvent(requestKey, json)
        }

        is NavDestination.PickCoordinate -> {
            val json = destination.result?.let {
                Json.encodeToString(it)
            }

            NavBaseAppDirections.pickDisplayCoordinate(requestKey, json)
        }

        is NavDestination.PickSwipeCoordinate -> {
            val json = destination.result?.let {
                Json.encodeToString(it)
            }

            NavBaseAppDirections.swipePickDisplayCoordinate(requestKey, json)
        }

        is NavDestination.PickPinchCoordinate -> {
            val json = destination.result?.let {
                Json.encodeToString(it)
            }

            NavBaseAppDirections.pinchPickDisplayCoordinate(requestKey, json)
        }

        is NavDestination.ConfigIntent -> {
            val json = destination.result?.let {
                Json.encodeToString(it)
            }

            NavBaseAppDirections.configIntent(requestKey, json)
        }

        is NavDestination.ChooseActivity -> NavBaseAppDirections.chooseActivity(requestKey)
        is NavDestination.ChooseSound -> NavBaseAppDirections.chooseSoundFile(requestKey)

        is NavDestination.ChooseBluetoothDevice -> NavBaseAppDirections.chooseBluetoothDevice(
            requestKey,
        )

        NavDestination.About -> NavBaseAppDirections.actionGlobalAboutFragment()
        NavDestination.Settings -> NavBaseAppDirections.toSettingsFragment()

        NavDestination.ShizukuSettings -> NavBaseAppDirections.toShizukuSettingsFragment()

        else -> throw IllegalArgumentException("Can not find a direction for this destination: $destination")
    }
}

fun NavigationViewModel.sendNavResultFromBundle(
    requestKey: String,
    destinationId: String,
    bundle: Bundle,
) {
    when (destinationId) {
        NavDestination.ID_CHOOSE_APP -> {
            val packageName = bundle.getString(ChooseAppFragment.EXTRA_PACKAGE_NAME)

            onNavResult(NavResult(requestKey, Json.encodeToString(packageName!!)))
        }

        NavDestination.ID_CHOOSE_APP_SHORTCUT -> {
            val json = bundle.getString(ChooseAppShortcutFragment.EXTRA_RESULT)

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_KEY_CODE -> {
            val keyCode = bundle.getInt(ChooseKeyCodeFragment.EXTRA_KEYCODE)

            onNavResult(NavResult(requestKey, Json.encodeToString(keyCode)))
        }

        NavDestination.ID_KEY_EVENT -> {
            val json = bundle.getString(ConfigKeyEventActionFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_PICK_COORDINATE -> {
            val json = bundle.getString(PickDisplayCoordinateFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_PICK_SWIPE_COORDINATE -> {
            val json = bundle.getString(SwipePickDisplayCoordinateFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_PICK_PINCH_COORDINATE -> {
            val json = bundle.getString(PinchPickDisplayCoordinateFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_CONFIG_INTENT -> {
            val json = bundle.getString(ConfigIntentFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_CHOOSE_ACTIVITY -> {
            val json = bundle.getString(ChooseActivityFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_CHOOSE_SOUND -> {
            val json = bundle.getString(ChooseSoundFileFragment.EXTRA_RESULT)!!

            onNavResult(NavResult(requestKey, json))
        }

        NavDestination.ID_CHOOSE_BLUETOOTH_DEVICE -> {
            val address = bundle.getString(ChooseBluetoothDeviceFragment.EXTRA_ADDRESS)!!
            val name = bundle.getString(ChooseBluetoothDeviceFragment.EXTRA_NAME)!!

            onNavResult(
                NavResult(
                    requestKey,
                    Json.encodeToString(BluetoothDeviceInfo(address, name)),
                ),
            )
        }
    }
}
