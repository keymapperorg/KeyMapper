package io.github.sds100.keymapper.purchasing

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class PurchasingManagerImpl(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : PurchasingManager

interface PurchasingManager
