package io.github.sds100.keymapper.base.utils.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistry
import com.airbnb.epoxy.EpoxyRecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.navigation.observeCurrentDestinationLiveData
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

abstract class RecyclerViewFragment<T, BINDING : ViewDataBinding> : Fragment() {

    companion object {
        private const val KEY_SAVED_STATE = "key_saved_state"

        private const val KEY_IS_APPBAR_VISIBLE = "key_is_app_visible"
        private const val KEY_SEARCH_STATE_KEY = "key_search_state_key"
    }

    abstract val listItems: Flow<State<List<T>>>

    open var isAppBarVisible = true
    open var searchStateKey: String? = null

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: BINDING? = null
    val binding: BINDING
        get() = _binding!!

    private val savedStateProvider = SavedStateRegistry.SavedStateProvider {
        Bundle().apply {
            putBoolean(KEY_IS_APPBAR_VISIBLE, isAppBarVisible)
            putString(KEY_SEARCH_STATE_KEY, searchStateKey)
        }
    }

    private val isSearchEnabled: Boolean
        get() = searchStateKey != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedStateRegistry.registerSavedStateProvider(KEY_SAVED_STATE, savedStateProvider)

        savedStateRegistry.consumeRestoredStateForKey(KEY_SAVED_STATE)?.apply {
            isAppBarVisible = getBoolean(KEY_IS_APPBAR_VISIBLE)
            searchStateKey = getString(KEY_SEARCH_STATE_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = bind(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime(),
                )
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.apply {
            // initially only show the progress bar
            getProgressBar(binding).isVisible = true
            getRecyclerView(binding).isVisible = true
            getEmptyListPlaceHolderTextView(binding).isVisible = false

            if (searchStateKey != null) {
                findNavController().observeCurrentDestinationLiveData<String>(
                    viewLifecycleOwner,
                    searchStateKey!!,
                ) {
                    onSearchQuery(it)
                }
            }

            subscribeUi(binding)

            getBottomAppBar(binding)?.let {
                it.isVisible = isAppBarVisible

                it.setNavigationOnClickListener {
                    onBackPressed()
                }
            }

            setupSearchView(binding)

            if (!requireActivity().onBackPressedDispatcher.hasEnabledCallbacks()) {
                // don't override back button if another fragment is controlling the app bar
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    if (isAppBarVisible) {
                        onBackPressed()
                    }
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            listItems.collectLatest { state ->
                when (state) {
                    is State.Data -> {
                        val recyclerView = getRecyclerView(binding)
                        if (state.data.isEmpty()) {
                            getProgressBar(binding).visibility = View.INVISIBLE

                            /*
                            Use INVISIBLE rather than GONE so that the previous list items don't flash briefly before
                            the new items are populated
                             */
                            recyclerView.visibility = View.INVISIBLE
                            getEmptyListPlaceHolderTextView(binding).visibility = View.VISIBLE

                            /*
                             Don't clear the recyclerview here because if a custom epoxy controller is set then
                             it will be cleared which means no items are shown when a request to populate it
                             is made again.
                             */
                            populateList(recyclerView, emptyList())
                        } else {
                            getProgressBar(binding).visibility = View.VISIBLE
                            getEmptyListPlaceHolderTextView(binding).visibility = View.INVISIBLE

                            /*
                            Don't hide the recyclerview here because if the state changes in response to
                            an onclick event in the recyclerview then there isn't a smooth transition
                            between the states. E.g the ripple effect on a button or card doesn't complete
                             */
                            populateList(recyclerView, state.data)

                            getProgressBar(binding).visibility = View.INVISIBLE

                            // show the recyclerview once it has been populated
                            recyclerView.visibility = View.VISIBLE
                        }
                    }

                    is State.Loading -> {
                        getProgressBar(binding).visibility = View.VISIBLE
                        getRecyclerView(binding).visibility = View.INVISIBLE
                        getEmptyListPlaceHolderTextView(binding).visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }

    fun returnResult(vararg extras: Pair<String, Any?>) {
        setFragmentResult(getRequestKey(), bundleOf(*extras))
        findNavController().navigateUp()
    }

    private fun setupSearchView(binding: BINDING) {
        getBottomAppBar(binding) ?: return

        val searchViewMenuItem = getBottomAppBar(binding)!!.menu.findItem(R.id.action_search)
        searchViewMenuItem ?: return

        searchViewMenuItem.isVisible = isSearchEnabled

        val searchView = searchViewMenuItem.actionView as SearchView

        searchStateKey ?: return

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                onSearchQuery(newText)

                return true
            }

            override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
        })
    }

    open fun onSearchQuery(query: String?) {}
    open fun getBottomAppBar(binding: BINDING): BottomAppBar? = null

    open fun onBackPressed() {
        findNavController().navigateUp()
    }

    open fun getRequestKey(): String = throw IllegalStateException("No request key is set")

    abstract fun getRecyclerView(binding: BINDING): EpoxyRecyclerView
    abstract fun getProgressBar(binding: BINDING): View
    abstract fun getEmptyListPlaceHolderTextView(binding: BINDING): View
    abstract fun subscribeUi(binding: BINDING)
    abstract fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<T>)
    abstract fun bind(inflater: LayoutInflater, container: ViewGroup?): BINDING
}
