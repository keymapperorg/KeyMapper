package io.github.sds100.keymapper.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Created by sds100 on 10/03/2019.
 */

class BottomSheetView : BottomSheetDialogFragment() {

    companion object {
        private val TAG = this::class.java.simpleName
        private const val KEY_LAYOUT_ID = "key_layout_id"

        fun create(@LayoutRes layoutId: Int): BottomSheetView {
            return BottomSheetView().apply {
                arguments = bundleOf(KEY_LAYOUT_ID to layoutId)
            }
        }

        fun show(activity: AppCompatActivity, @LayoutRes layoutId: Int) {
            create(layoutId).show(activity.supportFragmentManager, TAG)
        }
    }

    var onViewCreated: (view: View) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutId = arguments!!.getInt(KEY_LAYOUT_ID)

        return LayoutInflater.from(context).inflate(layoutId, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onViewCreated(view)
    }

    fun show(activity: FragmentActivity) = show(activity.supportFragmentManager, TAG)
}