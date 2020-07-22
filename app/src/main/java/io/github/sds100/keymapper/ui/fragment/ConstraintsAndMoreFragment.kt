package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.databinding.FragmentConstraintsAndMoreBinding
import splitties.experimental.ExperimentalSplittiesApi

/**
 * Created by sds100 on 19/03/2020.
 */
@ExperimentalSplittiesApi
class ConstraintsAndMoreFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentConstraintsAndMoreBinding.inflate(inflater, container, false).root
    }
}
