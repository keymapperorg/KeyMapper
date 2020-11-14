package io.github.sds100.keymapper.service

import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.annotation.RequiresApi

/**
 * Created by sds100 on 14/11/20.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class NotificationReceiver : NotificationListenerService()