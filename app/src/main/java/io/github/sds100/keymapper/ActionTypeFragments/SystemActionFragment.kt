package io.github.sds100.keymapper.ActionTypeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Adapters.SystemActionAdapter
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import kotlinx.android.synthetic.main.action_type_recyclerview.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which displays a list of all actions that can be performed on the system
 */
class SystemActionFragment : FilterableActionTypeFragment(),
        OnItemClickListener<SystemActionDef> {

    private val mSystemActionAdapter by lazy {
        SystemActionAdapter(
                ctx = context!!,
                onItemClickListener = this
        )
    }

    override val filterable: Filterable?
        get() = mSystemActionAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mSystemActionAdapter
    }

    override fun onItemClick(item: SystemActionDef) {
        val action = Action(ActionType.SYSTEM_ACTION, item.id)
        chooseSelectedAction(action)
    }
}