package io.github.sds100.keymapper.base.settings

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.common.utils.SettingsUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AppLocaleAdapterImpl @Inject constructor(
    @param:ApplicationContext private val ctx: Context,
) : AppLocaleAdapter {

    private val _currentLocaleDisplayName = MutableStateFlow(getLocaleDisplayName())
    override val currentLocaleDisplayName: StateFlow<String?> =
        _currentLocaleDisplayName.asStateFlow()

    fun invalidate() {
        _currentLocaleDisplayName.value = getLocaleDisplayName()
    }

    private fun getLocaleDisplayName(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }

        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            ctx.getString(R.string.language_system_default)
        } else {
            locales.get(0)?.displayName
        }
    }

    override fun launchAppLocaleSettingsScreen(): Boolean {
        return SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_APP_LOCALE_SETTINGS,
            uri = "package:${ctx.packageName}".toUri(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getSupportedLocales(): List<AppLocaleOption> {
        return buildList {
            val localeList = AppCompatDelegate.getApplicationLocales()
            for (i in 0 until localeList.size()) {
                val locale = localeList.get(i) ?: continue

                add(AppLocaleOption(locale.toLanguageTag(), locale.displayName))
            }
        }
    }

    override fun getCurrentLocale(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            null
        } else {
            locales.toLanguageTags()
        }
    }

    override fun setLocale(localeTag: String?) {
        val localeList = if (localeTag.isNullOrEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}

/**
 * Represents a locale option for the language picker.
 * @param tag The locale tag (e.g., "en", "pt") or null for system default.
 * @param displayName The display name shown to users.
 */
data class AppLocaleOption(val tag: String?, val displayName: String)

/**
 * Adapter to manage app locale settings using AndroidX AppCompatDelegate.
 * This integrates with Android 13+ per-app language preferences and provides
 * backward compatibility for older Android versions.
 */
interface AppLocaleAdapter {
    /**
     * The display name of the current locale, or null if not supported (Android < 13).
     */
    val currentLocaleDisplayName: StateFlow<String?>

    fun launchAppLocaleSettingsScreen(): Boolean

    /**
     * Get list of supported locales by parsing the locales_config.xml file.
     * Returns a list of AppLocaleOption with locale tag and display name.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getSupportedLocales(): List<AppLocaleOption>

    /**
     * Get the currently selected locale tag, or null if using system default.
     */
    fun getCurrentLocale(): String?

    /**
     * Set the app locale.
     * @param localeTag The locale tag (e.g., "en", "pt", "tr") or null for system default.
     */
    fun setLocale(localeTag: String?)
}
