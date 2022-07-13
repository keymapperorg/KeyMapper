package io.github.sds100.keymapper.actions

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Created by sds100 on 12/07/2022.
 */

@Composable
fun ChooseActionScreen(viewModel: ChooseActionViewModel2 = hiltViewModel(), onActionChosen: (ActionData) -> Unit) {
    Button(onClick = { onActionChosen(ActionData.ShowVolumeDialog) }, Modifier.wrapContentSize()) {
        Text("test")
    }
}