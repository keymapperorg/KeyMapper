package io.github.sds100.keymapper.util

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 15/03/2021.
 */

@Parcelize
data class ActivityInfo(val activityName: String, val packageName: String) : Parcelable
