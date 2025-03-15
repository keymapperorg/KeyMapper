package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.BaseMappingListItemCreatorOld
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 19/03/2021.
 */
class FingerprintMapListItemCreator(
    private val display: DisplaySimpleMappingUseCase,
    resourceProvider: ResourceProvider,
) : BaseMappingListItemCreatorOld<FingerprintMap, FingerprintMapAction>(
    display,
    FingerprintMapActionUiHelper(display, resourceProvider),
    resourceProvider,
) {

    fun create(
        fingerprintMap: FingerprintMap,
        showDeviceDescriptors: Boolean,
    ): FingerprintMapListItem {
        val header = when (fingerprintMap.id) {
            FingerprintGestureType.SWIPE_DOWN -> getString(R.string.fingerprint_gesture_down)
            FingerprintGestureType.SWIPE_UP -> getString(R.string.fingerprint_gesture_up)
            FingerprintGestureType.SWIPE_LEFT -> getString(R.string.fingerprint_gesture_left)
            FingerprintGestureType.SWIPE_RIGHT -> getString(R.string.fingerprint_gesture_right)
        }

        val midDot = getString(R.string.middot)

        val optionsDescription = buildString {
            getOptionLabels(fingerprintMap).forEachIndexed { index, label ->
                if (index != 0) {
                    append(" $midDot ")
                }

                append(label)
            }
        }

        val actionChipList = getActionChipList(fingerprintMap, showDeviceDescriptors)
        val constraintChipList = getConstraintChipList(fingerprintMap)

        val extraInfo = createExtraInfoString(fingerprintMap, actionChipList, constraintChipList)

        return FingerprintMapListItem(
            id = fingerprintMap.id,
            header = header,
            actionChipList = actionChipList,
            constraintChipList = constraintChipList,
            optionsDescription = optionsDescription,
            isEnabled = fingerprintMap.isEnabled,
            extraInfo = extraInfo,
        )
    }

    private fun getOptionLabels(fingerprintMap: FingerprintMap): List<String> {
        val labels = mutableListOf<String>()

        if (fingerprintMap.isVibrateAllowed() && fingerprintMap.vibrate) {
            labels.add(getString(R.string.flag_vibrate))
        }

        if (fingerprintMap.showToast) {
            labels.add(getString(R.string.flag_show_toast))
        }

        return labels
    }
}
