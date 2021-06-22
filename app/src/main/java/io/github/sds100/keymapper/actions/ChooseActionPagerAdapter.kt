package io.github.sds100.keymapper.actions

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.system.SystemActionListFragment
import io.github.sds100.keymapper.system.apps.ChooseAppFragment
import io.github.sds100.keymapper.actions.tapscreen.PickDisplayCoordinateFragment
import io.github.sds100.keymapper.system.intents.ConfigIntentFragment
import io.github.sds100.keymapper.actions.keyevent.ChooseKeyFragment
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventFragment
import io.github.sds100.keymapper.actions.phone.ChoosePhoneNumberFragment
import io.github.sds100.keymapper.actions.url.ChooseUrlFragment
import io.github.sds100.keymapper.util.ui.TabFragmentModel
import io.github.sds100.keymapper.actions.keyevent.KeyCodeListFragment
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileFragment
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutFragment
import io.github.sds100.keymapper.actions.text.TextBlockActionTypeFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class ChooseActionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    val tabFragmentCreators: List<TabFragmentModel> = mutableListOf(
        TabFragmentModel(R.string.action_type_title_application, ChooseAppFragment.SEARCH_STATE_KEY) {
            ChooseAppFragment().apply {
                isAppBarVisible = false
            }
        },
        TabFragmentModel(R.string.action_type_title_application_shortcut,
            ChooseAppShortcutFragment.SEARCH_STATE_KEY) {

            ChooseAppShortcutFragment().apply {
                isAppBarVisible = false
            }
        },
        TabFragmentModel(
            R.string.action_type_title_key_code,
            KeyCodeListFragment.SEARCH_STATE_KEY
        ) {

            KeyCodeListFragment().apply {
                isAppBarVisible = false
            }
        },

        TabFragmentModel(
            R.string.action_type_title_system_action,
            SystemActionListFragment.SEARCH_STATE_KEY
        ) {

            SystemActionListFragment().apply {
                isAppBarVisible = false
            }
        },

        TabFragmentModel(R.string.action_type_title_key, null) {
            ChooseKeyFragment()
        },

        TabFragmentModel(R.string.action_type_title_keyevent, null) {
            ConfigKeyEventFragment()
        },

        TabFragmentModel(R.string.action_type_title_text_block, null) {
            TextBlockActionTypeFragment()
        },

        TabFragmentModel(R.string.action_type_url, null) {
            ChooseUrlFragment()
        },

        TabFragmentModel(R.string.action_type_intent, null) {
            ConfigIntentFragment()
        },

        TabFragmentModel(R.string.action_type_phone_call, null) {
            ChoosePhoneNumberFragment()
        },

        TabFragmentModel(R.string.action_type_sound, null) {
            ChooseSoundFileFragment()
        },

        TabFragmentModel(R.string.tab_unsupported_actions, null) {

            UnsupportedActionListFragment().apply {
                isAppBarVisible = false
            }
        }
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val model = TabFragmentModel(R.string.action_type_tap_coordinate, null) {
                PickDisplayCoordinateFragment()
            }

            add(5, model)
        }
    }

    override fun getItemCount() = tabFragmentCreators.size

    override fun createFragment(position: Int) =
        tabFragmentCreators[position].fragmentCreator.invoke()
}