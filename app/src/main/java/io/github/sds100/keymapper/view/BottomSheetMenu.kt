package io.github.sds100.keymapper.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Created by sds100 on 10/03/2019.
 */

class BottomSheetMenu : BottomSheetDialogFragment() {

    companion object {
        private val TAG = this::class.java.simpleName
        private const val KEY_LAYOUT_ID = "key_layout_id"

        fun create(@LayoutRes layoutId: Int): BottomSheetMenu {
            return BottomSheetMenu().apply {
                arguments = bundleOf(KEY_LAYOUT_ID to layoutId)
            }
        }

        fun show(activity: AppCompatActivity, @LayoutRes layoutId: Int) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) {
                create(layoutId).show(activity.supportFragmentManager, TAG)
            }
        }
    }

    var createView: (view: View) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutId = arguments!!.getInt(KEY_LAYOUT_ID)

        return LayoutInflater.from(context).inflate(layoutId, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createView(view)
    }

    override fun onResume() {
        super.onResume()

        view?.let {
            createView(it)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        //force expand the bottom sheet when it is initially shown
        (dialog as BottomSheetDialog).apply {
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    fun show(activity: FragmentActivity) {
        activity.apply {
            if (supportFragmentManager.findFragmentByTag(TAG) == null) {
                show(supportFragmentManager, TAG)
            }
        }
    }
}