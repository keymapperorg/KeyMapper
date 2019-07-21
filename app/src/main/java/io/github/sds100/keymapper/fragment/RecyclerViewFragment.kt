package io.github.sds100.keymapper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.recyclerview_fragment.*

/**
 * Created by sds100 on 20/07/2019.
 */

abstract class RecyclerViewFragment : Fragment() {
    abstract val adapter: RecyclerView.Adapter<*>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = adapter
    }
}