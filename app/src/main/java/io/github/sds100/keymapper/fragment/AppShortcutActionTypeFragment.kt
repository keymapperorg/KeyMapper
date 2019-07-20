package io.github.sds100.keymapper.fragment

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Action.Companion.EXTRA_PACKAGE_NAME
import io.github.sds100.keymapper.Action.Companion.EXTRA_SHORTCUT_TITLE
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Extra
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.adapter.AppShortcutAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.util.AppShortcutUtils
import io.github.sds100.keymapper.view.editTextDialog
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 31/07/2018.
 */

/**
 * A Fragment which shows all the available shortcut widgets for each installed app which has them
 */
class AppShortcutActionTypeFragment : FilterableActionTypeFragment(), OnItemClickListener<ResolveInfo> {

    companion object {
        private const val REQUEST_CODE_SHORTCUT_CONFIGURATION = 837
    }

    private val mAppShortcutAdapter by lazy {
        AppShortcutAdapter(
                onItemClickListener = this,
                mAppShortcutList = AppShortcutUtils.getAppShortcuts(context!!.packageManager),
                mPackageManager = context!!.packageManager)
    }

    private var mTempShortcutPackageName: String? = null

    override val filterable
        get() = mAppShortcutAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mAppShortcutAdapter
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SHORTCUT_CONFIGURATION &&
                resultCode == Activity.RESULT_OK) {

            data ?: return

            val shortcutUri: String

            //the shortcut intents seem to be returned in 2 different formats.
            if (data.extras != null &&
                    data.extras!!.containsKey(Intent.EXTRA_SHORTCUT_INTENT)) {
                //get intent from selected shortcut
                val shortcutIntent = data.extras!!.get(Intent.EXTRA_SHORTCUT_INTENT) as Intent
                shortcutUri = shortcutIntent.toUri(0)

            } else {
                shortcutUri = data.toUri(0)
            }

            //show a dialog to prompt for a title.
            context!!.editTextDialog(
                    titleRes = R.string.dialog_title_create_shortcut_title
            ) { title ->
                val extras = mutableListOf(
                        Extra(EXTRA_SHORTCUT_TITLE, title),
                        Extra(EXTRA_PACKAGE_NAME, mTempShortcutPackageName!!)
                )

                //save the shortcut intent as a URI
                val action = Action(ActionType.APP_SHORTCUT, shortcutUri, extras)
                chooseSelectedAction(action)

                mTempShortcutPackageName = null
            }

        }
    }

    override fun onItemClick(item: ResolveInfo) {
        val packageName = item.activityInfo.applicationInfo.packageName
        mTempShortcutPackageName = packageName

        //open the shortcut configuration screen when the user taps a shortcut
        val intent = Intent()
        intent.setClassName(packageName, item.activityInfo.name)

        try {
            startActivityForResult(intent, REQUEST_CODE_SHORTCUT_CONFIGURATION)
        } catch (e: SecurityException) {
            context?.toast(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
        }
    }
}