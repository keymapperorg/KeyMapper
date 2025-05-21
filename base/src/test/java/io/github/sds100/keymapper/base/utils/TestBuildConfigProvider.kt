package io.github.sds100.keymapper.base.utils

import android.os.Build
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.common.BuildConfigProvider

class TestBuildConfigProvider : BuildConfigProvider {
    override val minApi: Int = Build.VERSION_CODES.LOLLIPOP
    override val maxApi: Int = 1000
    override val packageName: String = BuildConfig.LIBRARY_PACKAGE_NAME
    override val version: String = "1.0.0"
    override val versionCode: Int = 1
}
