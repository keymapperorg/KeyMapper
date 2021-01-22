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
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class RecyclerViewFragment<T, BINDING : ViewDataBinding> : Fragment() {

    companion object {
        private const val KEY_SAVED_STATE = "key_saved_state"

        private const val KEY_IS_APPBAR_VISIBLE = "key_is_app_visible"
        private const val KEY_REQUEST_KEY = "key_request_key"
        private const val KEY_SEARCH_STATE_KEY = "key_search_state_key"
    }

    private val savedStateProvider = SavedStateRegistry.SavedStateProvider {
        Bundle().apply {
            putBoolean(KEY_IS_APPBAR_VISIBLE, isAppBarVisible)
            putString(KEY_REQUEST_KEY, requestKey)
            putString(KEY_SEARCH_STATE_KEY, searchStateKey)
        }
    }

    private val isSearchEnabled: Boolean
        get() = searchStateKey != null

    abstract val modelState: IModelState<T>

    open var isAppBarVisible = true
    open var requestKey: String? = null
    open var searchStateKey: String? = null

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: BINDING? = null
    val binding: BINDING
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedStateRegistry.registerSavedStateProvider(KEY_SAVED_STATE, savedStateProvider)

        savedStateRegistry.consumeRestoredStateForKey(KEY_SAVED_STATE)?.apply {
            isAppBarVisible = getBoolean(KEY_IS_APPBAR_VISIBLE)
            requestKey = getString(KEY_REQUEST_KEY)
            searchStateKey = getString(KEY_SEARCH_STATE_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = bind(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            subscribeUi(binding)

            modelState.model.observe(viewLifecycleOwner, { model ->
                when (model) {
                    is Empty -> {

                        viewLifecycleScope.launchWhenResumed {
                            populateList(binding, null)
                        }

                        modelState.viewState.empty()
                    }
                    is Loading -> modelState.viewState.loading()
                    is Data -> {
                        modelState.viewState.loading()

                        viewLifecycleScope.launchWhenResumed {
                            populateList(binding, model.data)

                            modelState.viewState.populated()
                        }
                    }
                }
            })

            setupSearchView(binding)

            getBottomAppBar(binding)?.let {
                it.isVisible = isAppBarVisible

                it.setNavigationOnClickListener {
                    onBackPressed()
                }
            }

            if (isAppBarVisible) {
                //don't override back button if another fragment is controlling the app bar
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    onBackPressed()
                }
            }
        }
    }

    fun returnResult(vararg extras: Pair<String, Any?>) {
        requestKey?.let {
            setFragmentResult(it, bundleOf(*extras))
            findNavController().navigateUp()
        }
    }

    private fun setupSearchView(binding: BINDING) {
        getBottomAppBar(binding) ?: return

        val searchViewMenuItem = getBottomAppBar(binding)!!.menu.findItem(R.id.action_search)
        searchViewMenuItem.isVisible = isSearchEnabled

        if (isSearchEnabled) {
            findNavController().observeCurrentDestinationLiveData<String>(
                viewLifecycleOwner,
                searchStateKey!!
            ) {
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

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }

    open fun onSearchQuery(query: String?) {}
    open fun getBottomAppBar(binding: BINDING): BottomAppBar? {
        return null
    }

    open fun onBackPressed() {
        findNavController().navigateUp()
    }

    /**
     * [model] is null if it is empty.
     */
    abstract fun populateList(binding: BINDING, model: T?)
    abstract fun subscribeUi(binding: BINDING)
    abstract fun bind(inflater: LayoutInflater, container: ViewGroup?): BINDING
}