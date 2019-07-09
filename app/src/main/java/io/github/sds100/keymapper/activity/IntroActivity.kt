package io.github.sds100.keymapper.activity

import android.os.Bundle
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 07/07/2019.
 */

class IntroActivity : IntroActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(SimpleSlide.Builder().apply {
            title(R.string.showcase_note_from_the_developer)
            description(R.string.showcase_note_from_the_developer_message)
            background(R.color.red)
            backgroundDark(R.color.redDark)
            scrollable(false)
            canGoBackward(true)
        }.build())
    }
}