package io.github.sds100.keymapper.base.system.apps

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.system.apps.AppShortcutInfo
import io.github.sds100.keymapper.base.utils.Inject
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.RecyclerViewUtils
import io.github.sds100.keymapper.base.utils.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.base.utils.ui.showPopups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import splitties.toast.toast

class ChooseAppShortcutFragment : SimpleRecyclerViewFragment<AppShortcutListItem>() {

    companion object {
        const val REQUEST_KEY = "request_app_shortcut"
        const val EXTRA_RESULT = "extra_choose_app_shortcut_result"
        const val SEARCH_STATE_KEY = "key_app_shortcut_search_state"
    }

    private val args: ChooseAppShortcutFragmentArgs by navArgs()
    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val viewModel: ChooseAppShortcutViewModel by viewModels {
        Inject.chooseAppShortcutViewModel(requireContext())
    }

    override val listItems: Flow<State<List<AppShortcutListItem>>>
        get() = viewModel.state

    private val appShortcutConfigLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result ?: return@registerForActivityResult

            if (result.resultCode == Activity.RESULT_OK) {
                result.data ?: return@registerForActivityResult

                viewModel.onConfigureShortcutResult(result.data!!)
            }
        }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_RESULT to Json.encodeToString(it))
            }
        }

        viewModel.showPopups(this, binding)
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<AppShortcutListItem>,
    ) {
        binding.epoxyRecyclerView.withModels {
            listItems.forEach {
                simple {
                    id(it.id)
                    model(it)

                    onClickListener { _ ->
                        launchShortcutConfiguration(it.shortcutInfo)
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun getRequestKey(): String = args.requestKey

    private fun launchShortcutConfiguration(shortcutInfo: AppShortcutInfo) {
        Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            setClassName(shortcutInfo.packageName, shortcutInfo.activityName)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1)

            try {
                appShortcutConfigLauncher.launch(this)
            } catch (e: SecurityException) {
                toast(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
            }
        }
    }
}
