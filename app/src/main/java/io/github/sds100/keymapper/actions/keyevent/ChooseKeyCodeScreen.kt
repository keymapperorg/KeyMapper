package io.github.sds100.keymapper.actions.keyevent

import android.view.KeyEvent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator

/**
 * Created by sds100 on 28/07/2022.
 */

@Destination
@Composable
fun ChooseKeyCodeScreen(resultNavigator: ResultBackNavigator<Int>) {
    Button(onClick = {
        resultNavigator.navigateBack(KeyEvent.KEYCODE_VOLUME_DOWN)
    }) {
        Text("choose keycode")
    }
}