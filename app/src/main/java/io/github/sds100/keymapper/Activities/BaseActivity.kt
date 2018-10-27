package io.github.sds100.keymapper.Activities

import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by sds100 on 27/10/2018.
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 234
    }

    /**
     * The layout to show snackbars in.
     */
    abstract val layoutForSnackBar: View
}