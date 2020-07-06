package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistry
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.observeCurrentDestinationLiveData

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class RecyclerViewFragment<BINDING : ViewDataBinding> : Fragment() {

    companion object {
        private const val KEY_SAVED_STATE = "key_saved_state"

        private const val KEY_IS_APPBAR_VISIBLE = "key_is_app_visible"
        private const val KEY_IS_IN_PAGER_ADAPTER = "key_is_in_pager_adapter"
        private const val KEY_REQUEST_KEY = "key_request_key"
        private const val KEY_SEARCH_STATE_KEY = "key_search_state_key"
    }

    private val savedStateProvider = SavedStateRegistry.SavedStateProvider {
        Bundle().apply {
            putBoolean(KEY_IS_APPBAR_VISIBLE, isAppBarVisible)
            putBoolean(KEY_IS_IN_PAGER_ADAPTER, isInPagerAdapter)
            putSerializable(KEY_REQUEST_KEY, requestKey)
            putString(KEY_SEARCH_STATE_KEY, searchStateKey)
        }
    }

    private val mIsSearchEnabled: Boolean
        get() = searchStateKey != null

    var isAppBarVisible = true
    var isInPagerAdapter = false
    open var requestKey: String? = null
    open var searchStateKey: String? = null
    open val progressCallback: ProgressCallback? = null
    abstract val appBar: BottomAppBar
    lateinit var binding: BINDING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedStateRegistry.registerSavedStateProvider(KEY_SAVED_STATE, savedStateProvider)

        savedStateRegistry.consumeRestoredStateForKey(KEY_SAVED_STATE)?.apply {
            isAppBarVisible = getBoolean(KEY_IS_APPBAR_VISIBLE)
            isInPagerAdapter = getBoolean(KEY_IS_IN_PAGER_ADAPTER)
            requestKey = getString(KEY_REQUEST_KEY)
            searchStateKey = getString(KEY_SEARCH_STATE_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bind(inflater, container)

        subscribeList(binding)
        setupSearchView()

        appBar.isVisible = isAppBarVisible

        appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().navigateUp()
        }

        return binding.root
    }

    fun returnResult(vararg extras: Pair<String, Any?>) {
        requestKey?.let {
            setFragmentResult(it, bundleOf(*extras))
            findNavController().navigateUp()
        }
    }

    private fun setupSearchView() {
        val searchViewMenuItem = appBar.menu.findItem(R.id.action_search)
        searchViewMenuItem.isVisible = mIsSearchEnabled

        if (mIsSearchEnabled) {
            findNavController().observeCurrentDestinationLiveData<String>(viewLifecycleOwner, searchStateKey!!) {
                onSearchQuery(it)
            }

            val searchView = searchViewMenuItem.actionView as SearchView

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    onSearchQuery(newText)

                    return true
                }

                override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
            })
        }
    }

    open fun onSearchQuery(query: String?) {}
    abstract fun subscribeList(binding: BINDING)
    abstract fun bind(inflater: LayoutInflater,
                      container: ViewGroup?): BINDING
}