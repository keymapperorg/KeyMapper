package io.github.sds100.keymapper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ParceledListSlice
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.data.Gesture
import io.github.sds100.keymapper.data.TapGesture
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.util.KeyEventAction
import splitties.bitflags.hasFlag

@TargetApi(Build.VERSION_CODES.N)
class TapperAccessibilityService : AccessibilityService() {
    companion object {
        private const val ACTION_GESTURE = "$PACKAGE_NAME.ACTION_GESTURE"
        private const val EXTRA_GESTURE = "$PACKAGE_NAME.EXTRA_GESTURE"

        fun sendTapGesture(ctx: Context, gesture: Gesture) {
            val intent = Intent(ctx, TapperAccessibilityService::class.java)
            intent.putExtra(EXTRA_GESTURE, gesture)

            ctx.startService(intent)
        }
    }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        run {
            when (intent.action) {
                ACTION_GESTURE -> {
                    val gesture = intent.getParcelableExtra<Gesture?>(EXTRA_GESTURE) ?: return@run

                    val path = Path()
                    val duration = 1L //ms

                    val strokeDescription = when (gesture) {
                        is TapGesture -> {
                            path.moveTo(gesture.x, gesture.y)

                            if (gesture.action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)
                                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                when (gesture.keyEventAction) {
                                    KeyEventAction.DOWN -> GestureDescription.StrokeDescription(path, 0, duration, true)
                                    KeyEventAction.UP -> GestureDescription.StrokeDescription(path, 59999, duration, false)
                                    else -> null
                                }

                            } else {
                                GestureDescription.StrokeDescription(path, 0, duration)
                            }
                        }
                        else -> throw IllegalArgumentException("Invalid Gesture $gesture")
                    }

                    strokeDescription?.let {
                        val gestureDescription = GestureDescription.Builder().apply {
                            addStroke(it)
                        }.build()

                        dispatchGesture(gestureDescription, null, null)
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }
}