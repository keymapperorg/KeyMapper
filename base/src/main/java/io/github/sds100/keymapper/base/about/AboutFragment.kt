package io.github.sds100.keymapper.base.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.databinding.FragmentAboutBinding
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AboutFragment : Fragment() {

    @Inject
    lateinit var buildConfigProvider: BuildConfigProvider

    @Inject
    lateinit var purchasingManager: PurchasingManager

    @Inject
    lateinit var clipboardAdapter: ClipboardAdapter

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentAboutBinding? = null
    val binding: FragmentAboutBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime(),
                )
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onBackPressed()
        }

        binding.apply {
            appBar.setNavigationOnClickListener {
                onBackPressed()
            }

            version = "${buildConfigProvider.version} ${buildConfigProvider.versionCode}"

            layoutCustomerId.setOnClickListener {
                val customerId = this.customerId ?: return@setOnClickListener

                clipboardAdapter.copy(getString(R.string.about_customer_id_title), customerId)

                Toast.makeText(
                    requireContext(),
                    R.string.about_customer_id_copied_toast,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        // The row stays hidden if this fails, which is what happens in the FOSS build because
        // it has no purchasing implementation.
        viewLifecycleOwner.lifecycleScope.launch {
            purchasingManager.getCustomerId().onSuccess { customerId ->
                binding.customerId = customerId
            }
        }
    }

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }

    private fun onBackPressed() {
        findNavController().navigateUp()
    }
}
