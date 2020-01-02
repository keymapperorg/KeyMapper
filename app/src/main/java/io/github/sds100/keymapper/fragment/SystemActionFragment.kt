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
    }

    override fun onItemClick(item: SystemActionDef) {
        item.getOptions(ctx).onSuccess { options ->

            val optionLabels = options.map { optionId ->
                val optionLabel = Option.getOptionLabel(ctx, item.id, optionId)

                optionLabel ?: ctx.str(R.string.error_cant_find_option_label, optionId)
            }

            ctx.selector(items = optionLabels) { _, which ->
                val selectedOption = options[which]

                val action = Action(
                    type = ActionType.SYSTEM_ACTION,
                    data = item.id,
                    extra = Extra(Option.getExtraIdForOption(item.id), selectedOption)
                )

                val optionLabel = Option.getOptionLabel(ctx, item.id, selectedOption)

                if (optionLabel != null) {
                    when (item.id) {
                        SystemAction.SWITCH_KEYBOARD -> {
                            action.extras.add(Extra(Action.EXTRA_IME_NAME, optionLabel))
                        }
                    }
                }

                chooseSelectedAction(action)
            }

            return
        }

        val messageOnSelection = item.getMessageOnSelection(ctx)

        if (messageOnSelection != null) {
            context?.alert {
                title = item.getDescription(ctx)
                message = messageOnSelection
                okButton {
                    val action = Action(ActionType.SYSTEM_ACTION, item.id)
                    chooseSelectedAction(action)
                }
            }?.show()

            return
        }

        val action = Action(ActionType.SYSTEM_ACTION, item.id)
        chooseSelectedAction(action)
    }
}