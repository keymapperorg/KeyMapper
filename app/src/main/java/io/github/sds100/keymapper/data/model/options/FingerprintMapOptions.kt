package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.data.model.options.BoolOption.Companion.saveBoolOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.saveIntOption
import io.github.sds100.keymapper.util.result.valueOrNull
import kotlinx.android.parcel.Parcelize
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 18/11/20.
 */

@Parcelize
class FingerprintMapOptions(
    override val id: String,
    val vibrate: BoolOption,
    val vibrateDuration: IntOption

) : BaseOptions<FingerprintMap> {
    companion object {
        const val ID_VIBRATE = "vibrate"
        const val ID_VIBRATION_DURATION = "vibration_duration"
    }

    constructor(gestureId: String, fingerprintMap: FingerprintMap) : this(
        id = gestureId,

        vibrate = BoolOption(
            id = ID_VIBRATE,
            value = fingerprintMap.flags.hasFlag(FingerprintMap.FLAG_VIBRATE),
            isAllowed = true
        ),

        vibrateDuration = IntOption(
            id = ID_VIBRATION_DURATION,
            value = fingerprintMap.extras
                .getData(FingerprintMap.EXTRA_VIBRATION_DURATION).valueOrNull()?.toInt()
                ?: IntOption.DEFAULT,
            isAllowed = fingerprintMap.flags.hasFlag(FingerprintMap.FLAG_VIBRATE)
        )
    )

    override fun setValue(id: String, value: Boolean): FingerprintMapOptions {
        when (id) {
            ID_VIBRATE -> {
                vibrate.value = value
                vibrateDuration.isAllowed = value
            }
        }

        return this
    }

    override fun setValue(id: String, value: Int): FingerprintMapOptions {
        when (id) {
            ID_VIBRATION_DURATION -> vibrateDuration.value = value
        }

        return this
    }

    override val intOptions: List<IntOption>
        get() = listOf(vibrateDuration)

    override val boolOptions: List<BoolOption>
        get() = listOf(vibrate)

    override fun apply(old: FingerprintMap): FingerprintMap {
        val newFlags = old.flags
            .saveBoolOption(vibrate, FingerprintMap.FLAG_VIBRATE)

        val newExtras = old.extras
            .saveIntOption(vibrateDuration, FingerprintMap.EXTRA_VIBRATION_DURATION)

        return old.copy(flags = newFlags, extras = newExtras)
    }
}