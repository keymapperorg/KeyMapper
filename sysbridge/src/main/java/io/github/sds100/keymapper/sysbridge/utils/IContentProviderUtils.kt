package io.github.sds100.keymapper.sysbridge.utils

import android.content.AttributionSource
import android.content.IContentProvider
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle

internal object IContentProviderUtils {

    @Throws(android.os.RemoteException::class)
    fun callCompat(
        provider: IContentProvider,
        callingPkg: String?,
        authority: String?,
        method: String?,
        arg: String?,
        extras: Bundle?,
    ): Bundle? {
        val result: Bundle?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uid = android.system.Os.getuid()

            result = provider.call(
                (AttributionSource.Builder(uid)).setPackageName(callingPkg).build(),
                authority,
                method,
                arg,
                extras,
            )
        } else if (Build.VERSION.SDK_INT >= 30) {
            result =
                provider.call(callingPkg, null as String?, authority, method, arg, extras)
        } else if (Build.VERSION.SDK_INT >= 29) {
            result = provider.call(callingPkg, authority, method, arg, extras)
        } else {
            result = provider.call(callingPkg, method, arg, extras)
        }

        return result
    }

    @Throws(android.os.RemoteException::class)
    fun queryCompat(
        provider: IContentProvider,
        callingPkg: String?,
        url: Uri,
        projection: Array<String>?,
        queryArgs: Bundle?,
    ): Cursor? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uid = android.system.Os.getuid()
            provider.query(
                AttributionSource.Builder(uid).setPackageName(callingPkg).build(),
                url,
                projection,
                queryArgs,
                null,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            provider.query(callingPkg, null as String?, url, projection, queryArgs, null)
        } else {
            provider.query(callingPkg, url, projection, queryArgs, null)
        }
    }
}
