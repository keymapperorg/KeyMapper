package io.github.sds100.keymapper.onboarding

import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 14/04/2021.
 */
data class AppIntroSlideUi(
    val id: String,
    val image: Drawable,
    val backgroundColor: Int,
    val title: String,
    val description: String,
    val buttonId1: String? = null,
    val buttonText1: String? = null,
    val buttonId2: String? = null,
    val buttonText2: String? = null,
    val buttonId3: String? = null,
    val buttonText3: String? = null,
)
