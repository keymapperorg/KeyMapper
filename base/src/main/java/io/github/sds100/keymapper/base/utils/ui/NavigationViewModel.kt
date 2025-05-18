package io.github.sds100.keymapper.base.utils.ui

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ChooseActionFragment
import io.github.sds100.keymapper.mapping.actions.keyevent.ChooseKeyCodeFragment
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventActionFragment
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickCoordinateResult
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.actions.sound.ChooseSoundFileFragment
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickCoordinateResult
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.base.actions.tapscreen.PickDisplayCoordinateFragment
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementFragment
import io.github.sds100.keymapper.constraints.ChooseConstraintFragment
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.base.system.apps.ChooseActivityFragment
import io.github.sds100.keymapper.base.system.apps.ChooseAppFragment
import io.github.sds100.keymapper.base.system.apps.ChooseAppShortcutFragment
import io.github.sds100.keymapper.base.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.base.system.ChooseBluetoothDeviceFragment
import io.github.sds100.keymapper.base.system.intents.ConfigIntentFragment
import io.github.sds100.keymapper.base.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.common.utils.getJsonSerializable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json



class NavigationViewModelImpl : NavigationViewModel {
    private val _onNavResult by lazy { MutableSharedFlow<NavResult>() }
    override val onNavResult by lazy { _onNavResult.asSharedFlow() }

    private val _navigate = MutableSharedFlow<NavigateEvent>()
    override val navigate = _navigate.asSharedFlow()

    override suspend fun navigate(event: NavigateEvent) {
        // wait for the view to collect so navigating can happen
        _navigate.subscriptionCount.first { it > 0 }

        _navigate.emit(event)
    }

    override fun onNavResult(result: NavResult) {
        runBlocking { _onNavResult.emit(result) }
    }
}

interface NavigationViewModel {
    val navigate: SharedFlow<NavigateEvent>
    val onNavResult: SharedFlow<NavResult>

    fun onNavResult(result: NavResult)
    suspend fun navigate(event: NavigateEvent)
}

suspend inline fun <reified R> NavigationViewModel.navigate(
    key: String,
    destination: NavDestination<R>,
): R? {
    navigate(NavigateEvent(key, destination))

    /*
    This ensures only one job for a dialog is active at once by cancelling previous jobs when a new
    dialog is shown with the same key
     */
    return merge(
        navigate.dropWhile { it.key != key }.map { null },
        onNavResult.dropWhile { it.result !is R? && it.key != key }.map { it.result },
    ).first() as R?
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

    navigate.onEach { event ->
        val (requestKey, destination) = event

        pendingResults[requestKey] = destination.id

        fragment.clearFragmentResultListener(requestKey)

        fragment.setFragmentResultListener(requestKey) { _, bundle ->
            pendingResults.remove(event.key)
            sendNavResultFromBundle(event.key, event.destination.id, bundle)
        }

        val direction = when (destination) {
            is NavDestination.ChooseApp -> NavAppDirections.chooseApp(
                destination.allowHiddenApps,
                requestKey,
            )

            NavDestination.ChooseAppShortcut -> NavAppDirections.chooseAppShortcut(requestKey)
            NavDestination.ChooseKeyCode -> NavAppDirections.chooseKeyCode(requestKey)
            is NavDestination.ConfigKeyEventAction -> {
                val json = destination.action?.let {
                    Json.encodeToString(it)
                }

                NavAppDirections.configKeyEvent(requestKey, json)
            }

            is NavDestination.PickCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavAppDirections.pickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.PickSwipeCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavAppDirections.swipePickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.PickPinchCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavAppDirections.pinchPickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.ConfigIntent -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavAppDirections.configIntent(requestKey, json)
            }

            is NavDestination.ChooseActivity -> NavAppDirections.chooseActivity(requestKey)
            is NavDestination.ChooseSound -> NavAppDirections.chooseSoundFile(requestKey)
            NavDestination.ChooseAction -> NavAppDirections.toChooseActionFragment(requestKey)
            is NavDestination.ChooseConstraint -> NavAppDirections.chooseConstraint(
                requestKey = requestKey,
            )

            is NavDestination.ChooseBluetoothDevice -> NavAppDirections.chooseBluetoothDevice(
                requestKey,
            )

            NavDestination.About -> NavAppDirections.actionGlobalAboutFragment()
            NavDestination.Settings -> NavAppDirections.toSettingsFragment()

            is NavDestination.ConfigKeyMap -> when (destination) {
                is NavDestination.ConfigKeyMap.New ->
                    NavAppDirections.actionToConfigKeymap(
                        groupUid = destination.groupUid,
                        showAdvancedTriggers = destination.showAdvancedTriggers,
                    )

                is NavDestination.ConfigKeyMap.Open ->
                    NavAppDirections.actionToConfigKeymap(
                        keyMapUid = destination.keyMapUid,
                        showAdvancedTriggers = destination.showAdvancedTriggers,
                    )
            }

            is NavDestination.ChooseFloatingLayout -> NavAppDirections.toChooseFloatingLayoutFragment()
            NavDestination.ShizukuSettings -> NavAppDirections.toShizukuSettingsFragment()
            is NavDestination.ConfigFloatingButton -> NavAppDirections.toConfigFloatingButton(
                destination.buttonUid,
            )

            is NavDestination.InteractUiElement -> NavAppDirections.interactUiElement(
                requestKey = requestKey,
                action = destination.action?.let { Json.encodeToString(destination.action) },
            )
        }

        fragment.findNavController().navigate(direction)
    }.launchIn(fragment.lifecycleScope)
}

fun NavigationViewModel.sendNavResultFromBundle(
    requestKey: String,
    destinationId: String,
    bundle: Bundle,
) {
    when (destinationId) {
        NavDestination.ID_CHOOSE_APP -> {
            val packageName = bundle.getString(ChooseAppFragment.EXTRA_PACKAGE_NAME)

            onNavResult(NavResult(requestKey, packageName!!))
        }

        NavDestination.ID_CHOOSE_APP_SHORTCUT -> {
            val result = bundle.getJsonSerializable<ChooseAppShortcutResult>(
                ChooseAppShortcutFragment.EXTRA_RESULT,
            )

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_KEY_CODE -> {
            val keyCode = bundle.getInt(io.github.sds100.keymapper.mapping.actions.keyevent.ChooseKeyCodeFragment.EXTRA_KEYCODE)

            onNavResult(NavResult(requestKey, keyCode))
        }

        NavDestination.ID_KEY_EVENT -> {
            val json = bundle.getString(ConfigKeyEventActionFragment.EXTRA_RESULT)!!
            val keyEventAction = Json.decodeFromString<ActionData.InputKeyEvent>(json)

            onNavResult(NavResult(requestKey, keyEventAction))
        }

        NavDestination.ID_PICK_COORDINATE -> {
            val json = bundle.getString(PickDisplayCoordinateFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<PickCoordinateResult>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_PICK_SWIPE_COORDINATE -> {
            val json = bundle.getString(SwipePickDisplayCoordinateFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<SwipePickCoordinateResult>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_PICK_PINCH_COORDINATE -> {
            val json = bundle.getString(PinchPickDisplayCoordinateFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<PinchPickCoordinateResult>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_CONFIG_INTENT -> {
            val json = bundle.getString(ConfigIntentFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<ConfigIntentResult>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_CHOOSE_ACTIVITY -> {
            val json = bundle.getString(ChooseActivityFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<ActivityInfo>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_CHOOSE_SOUND -> {
            val json = bundle.getString(ChooseSoundFileFragment.EXTRA_RESULT)!!
            val result = Json.decodeFromString<ActionData.Sound>(json)

            onNavResult(NavResult(requestKey, result))
        }

        NavDestination.ID_CHOOSE_ACTION -> {
            val json = bundle.getString(ChooseActionFragment.EXTRA_ACTION)!!
            val action = Json.decodeFromString<ActionData>(json)

            onNavResult(NavResult(requestKey, action))
        }

        NavDestination.ID_CHOOSE_CONSTRAINT -> {
            val json = bundle.getString(ChooseConstraintFragment.EXTRA_CONSTRAINT)!!
            val constraint = Json.decodeFromString<Constraint>(json)

            onNavResult(NavResult(requestKey, constraint))
        }

        NavDestination.ID_CHOOSE_BLUETOOTH_DEVICE -> {
            val address = bundle.getString(ChooseBluetoothDeviceFragment.EXTRA_ADDRESS)!!
            val name = bundle.getString(ChooseBluetoothDeviceFragment.EXTRA_NAME)!!

            onNavResult(NavResult(requestKey, BluetoothDeviceInfo(address, name)))
        }

        NavDestination.ID_INTERACT_UI_ELEMENT_ACTION -> {
            val json = bundle.getString(InteractUiElementFragment.EXTRA_ACTION)!!
            val result = Json.decodeFromString<ActionData.InteractUiElement>(json)
            onNavResult(NavResult(requestKey, result))
        }
    }
}
