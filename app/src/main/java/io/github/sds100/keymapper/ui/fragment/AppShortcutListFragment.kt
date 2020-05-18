package io.github.sds100.keymapper.ui.fragment

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.model.AppShortcutModel
import io.github.sds100.keymapper.data.viewmodel.AppShortcutListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.editTextAlertDialog
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.toast.toast


/**
 * Created by sds100 on 29/03/2020.
 */

class AppShortcutListFragment : RecyclerViewFragment() {

    companion object {
        private const val REQUEST_CODE_SHORTCUT_CONFIG = 837

        const val SAVED_STATE_KEY = "key_app_shortcut"
        const val SEARCH_STATE_KEY = "key_app_shortcut_search_state"
    }

    override var selectedModelKey: String? = SAVED_STATE_KEY
    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val mViewModel: AppShortcutListViewModel by viewModels {
        InjectorUtils.provideAppShortcutListViewModel(requireContext())
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        mViewModel.filteredAppShortcutModelList.observe(viewLifecycleOwner) { appShortcutList ->

            binding.epoxyRecyclerView.withModels {
                appShortcutList.forEach {
                    simple {
                        id(it.activityInfo.name)
                        primaryText(it.label)
                        icon(it.icon)

                        onClick { _ ->
                            it.activityInfo.launchShortcutConfiguration()
                        }
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        mViewModel.searchQuery.value = query
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SHORTCUT_CONFIG && resultCode == Activity.RESULT_OK) {

            data ?: return

            val uri: String

            //the shortcut intents seem to be returned in 2 different formats.
            if (data.extras != null &&
                data.extras!!.containsKey(Intent.EXTRA_SHORTCUT_INTENT)) {
                //get intent from selected shortcut
                val shortcutIntent = data.extras!!.get(Intent.EXTRA_SHORTCUT_INTENT) as Intent
                uri = shortcutIntent.toUri(0)

            } else {
                uri = data.toUri(0)
            }

            val packageName = Intent.parseUri(uri, 0).`package`
                ?: data.component?.packageName
                ?: Intent.parseUri(uri, 0).component?.packageName!!

            lifecycleScope.launch {
                val shortcutName = getShortcutName(data)
                val appName = SystemRepository.getInstance(requireContext()).getAppName(packageName)

                val model = AppShortcutModel("$appName: $shortcutName", packageName, uri)

                selectModel(model)
            }
        }
    }

    private fun ActivityInfo.launchShortcutConfiguration() {
        Intent().apply {
            action = Intent.ACTION_CREATE_SHORTCUT
            setClassName(packageName, name)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1)

            try {
                startActivityForResult(this, REQUEST_CODE_SHORTCUT_CONFIG)
            } catch (e: SecurityException) {
                toast(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
            }
        }
    }

    private suspend fun getShortcutName(data: Intent): String {
        var shortcutName: String? = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)

        if (shortcutName.isNullOrBlank()) {
            shortcutName = withContext(lifecycleScope.coroutineContext) {
                requireActivity().editTextAlertDialog(str(R.string.dialog_title_create_shortcut_title))
            }
        }

        return shortcutName
    }
}