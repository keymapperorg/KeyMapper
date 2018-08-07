package io.github.sds100.keymapper.ActionTypeFragments

import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Adapters.AppShortcutAdapter
import io.github.sds100.keymapper.Adapters.SimpleItemAdapter
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_recyclerview.*

/**
 * Created by sds100 on 31/07/2018.
 */

/**
 * A Fragment which shows all the available shortcut widgets for each installed app which has them
 */
class AppShortcutActionTypeFragment : ActionTypeFragment(),
        SimpleItemAdapter.OnItemClickListener<ResolveInfo> {

    private val mAppShortcutAdapter by lazy {
        AppShortcutAdapter(
                context!!.packageManager, onItemClickListener = this)
    }

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
        recyclerView.adapter = mAppShortcutAdapter
    }

    override fun onItemClick(item: ResolveInfo) {
        val action = Action(ActionType.APP_SHORTCUT, item.activityInfo.name)
        chooseSelectedAction(action)
    }
}