package io.github.sds100.keymapper.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.adapter.SystemActionAdapter
import io.github.sds100.keymapper.interfaces.IContext
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.util.SystemActionUtils
import io.github.sds100.keymapper.util.str
import kotlinx.android.synthetic.main.action_type_system_action.*
import kotlinx.android.synthetic.main.recyclerview_fragment.recyclerView
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.selector

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which displays a list of all actions that can be performed on the system
 */
class SystemActionFragment : FilterableActionTypeFragment(),
    OnItemClickListener<SystemActionDef>, IContext {

    private val mSystemActionAdapter by lazy {
        SystemActionAdapter(
            iContext = this,
            onItemClickListener = this
        )
    }

    override val ctx: Context
        get() = context!!

    override val filterable: Filterable?
        get() = mSystemActionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_system_action, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.adapter = mSystemActionAdapter

        val areAllActionsSupported = SystemActionUtils.areAllActionsSupported(ctx)
        textViewUnsupportedActions.isVisible = !areAllActionsSupported

        if (!areAllActionsSupported) {

        }
    }

    override fun onItemClick(systemAction: SystemActionDef) {
        if (systemAction.hasOptions) {
            val items = systemAction.options.map { ctx.str(SystemActionUtils.getTextForOptionId(it)) }

            ctx.selector(items = items) { _, which ->
                val selectedOption = systemAction.options[which]

                val action = Action(
                    type = ActionType.SYSTEM_ACTION,
                    data = systemAction.id,
                    extra = Extra(Option.getExtraIdForOption(systemAction.id), selectedOption)
                )

                chooseSelectedAction(action)
            }

            return
        }

        if (systemAction.messageOnSelection != null) {
            context?.alert {
                titleResource = systemAction.descriptionRes
                messageResource = systemAction.messageOnSelection
                okButton {
                    val action = Action(ActionType.SYSTEM_ACTION, systemAction.id)
                    chooseSelectedAction(action)
                }
            }?.show()

            return
        }

        val action = Action(ActionType.SYSTEM_ACTION, systemAction.id)
        chooseSelectedAction(action)
    }
}