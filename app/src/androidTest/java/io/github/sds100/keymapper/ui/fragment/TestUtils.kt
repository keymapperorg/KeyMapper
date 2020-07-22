package io.github.sds100.keymapper

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher


/**
 * Created by sds100 on 22/05/20.
 */

fun clickChildViewWithId(id: Int): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View>? {
            return null
        }

        override fun getDescription(): String {
            return "Click on a child view with specified id."
        }

        override fun perform(uiController: UiController, view: View) {
            val v: View = view.findViewById(id)
            v.performClick()
        }
    }
}
