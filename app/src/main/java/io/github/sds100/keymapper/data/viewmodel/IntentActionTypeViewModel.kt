package io.github.sds100.keymapper.data.viewmodel

import android.content.Intent
import android.os.Build
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.IntentExtraListItemModel
import io.github.sds100.keymapper.data.model.IntentExtraModel
import io.github.sds100.keymapper.data.model.IntentExtraType
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 01/01/21.
 */
class IntentActionTypeViewModel : ViewModel() {
    val targetActivity = MutableLiveData(false)
    val targetBroadcastReceiver = MutableLiveData(false)
    val targetService = MutableLiveData(false)

    val description = MutableLiveData("")
    val action = MutableLiveData("")
    val categoriesString = MutableLiveData("")
    val categoriesList = categoriesString.map {
        it.split(',')
    }

    val flags = MutableLiveData("")

    val data = MutableLiveData("")
    val targetPackage = MutableLiveData("")
    val targetClass = MutableLiveData("")

    val showChooseActivityButton = targetActivity

    private val _extras = MutableLiveData(emptyList<IntentExtraModel>())
    val extras: LiveData<List<IntentExtraModel>> = _extras

    private val _extrasListItemModels =
        MutableLiveData<DataState<List<IntentExtraListItemModel>>>(Empty())
    val extrasListItemModels: LiveData<DataState<List<IntentExtraListItemModel>>> =
        _extrasListItemModels

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(_extras) {
            value = BuildIntentExtraListItemModels(it)
        }
    }
    val eventStream: LiveData<Event> = _eventStream

    val availableIntentFlags: List<Pair<Int, String>> =
        sequence {
            yield(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT to "FLAG_ACTIVITY_BROUGHT_TO_FRONT")
            yield(Intent.FLAG_ACTIVITY_CLEAR_TASK to "FLAG_ACTIVITY_CLEAR_TASK")
            yield(Intent.FLAG_ACTIVITY_CLEAR_TOP to "FLAG_ACTIVITY_CLEAR_TOP")

            yield(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET to "FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET")

            yield(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS to "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS")
            yield(Intent.FLAG_ACTIVITY_FORWARD_RESULT to "FLAG_ACTIVITY_FORWARD_RESULT")
            yield(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY to "FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                yield(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT to "FLAG_ACTIVITY_LAUNCH_ADJACENT")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                yield(Intent.FLAG_ACTIVITY_MATCH_EXTERNAL to "FLAG_ACTIVITY_MATCH_EXTERNAL")
            }

            yield(Intent.FLAG_ACTIVITY_MULTIPLE_TASK to "FLAG_ACTIVITY_MULTIPLE_TASK")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                yield(Intent.FLAG_ACTIVITY_NEW_DOCUMENT to "FLAG_ACTIVITY_NEW_DOCUMENT")
            }

            yield(Intent.FLAG_ACTIVITY_NEW_TASK to "FLAG_ACTIVITY_NEW_TASK")
            yield(Intent.FLAG_ACTIVITY_NO_ANIMATION to "FLAG_ACTIVITY_NO_ANIMATION")
            yield(Intent.FLAG_ACTIVITY_NO_HISTORY to "FLAG_ACTIVITY_NO_HISTORY")
            yield(Intent.FLAG_ACTIVITY_NO_USER_ACTION to "FLAG_ACTIVITY_NO_USER_ACTION")
            yield(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP to "FLAG_ACTIVITY_PREVIOUS_IS_TOP")
            yield(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT to "FLAG_ACTIVITY_REORDER_TO_FRONT")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                yield(Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT to "FLAG_ACTIVITY_REQUIRE_DEFAULT")
                yield(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER to "FLAG_ACTIVITY_REQUIRE_NON_BROWSER")
            }

            yield(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED to "FLAG_ACTIVITY_RESET_TASK_IF_NEEDED")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                yield(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS to "FLAG_ACTIVITY_RETAIN_IN_RECENTS")
            }

            yield(Intent.FLAG_ACTIVITY_SINGLE_TOP to "FLAG_ACTIVITY_SINGLE_TOP")
            yield(Intent.FLAG_ACTIVITY_TASK_ON_HOME to "FLAG_ACTIVITY_TASK_ON_HOME")
            yield(Intent.FLAG_DEBUG_LOG_RESOLUTION to "FLAG_DEBUG_LOG_RESOLUTION")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                yield(Intent.FLAG_DIRECT_BOOT_AUTO to "FLAG_DIRECT_BOOT_AUTO")
            }

            yield(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES to "FLAG_EXCLUDE_STOPPED_PACKAGES")
            yield(Intent.FLAG_FROM_BACKGROUND to "FLAG_FROM_BACKGROUND")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                yield(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION to "FLAG_GRANT_PERSISTABLE_URI_PERMISSION")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                yield(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION to "FLAG_GRANT_PREFIX_URI_PERMISSION")
            }

            yield(Intent.FLAG_GRANT_READ_URI_PERMISSION to "FLAG_GRANT_READ_URI_PERMISSION")
            yield(Intent.FLAG_GRANT_WRITE_URI_PERMISSION to "FLAG_GRANT_WRITE_URI_PERMISSION")
            yield(Intent.FLAG_INCLUDE_STOPPED_PACKAGES to "FLAG_INCLUDE_STOPPED_PACKAGES")
            yield(Intent.FLAG_RECEIVER_FOREGROUND to "FLAG_RECEIVER_FOREGROUND")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                yield(Intent.FLAG_RECEIVER_NO_ABORT to "FLAG_RECEIVER_NO_ABORT")
            }

            yield(Intent.FLAG_RECEIVER_REGISTERED_ONLY to "FLAG_RECEIVER_REGISTERED_ONLY")
            yield(Intent.FLAG_RECEIVER_REPLACE_PENDING to "FLAG_RECEIVER_REPLACE_PENDING")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                yield(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS to "FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS")
            }

        }.toList()

    private val _isValid = MediatorLiveData<Boolean>().apply {

        fun invalidate() {
            if (description.value.isNullOrEmpty()) {
                value = false
                return
            }

            if (_extras.value?.any { !it.isValidValue || it.name.isEmpty() } == true) {
                value = false
                return
            }

            value = true
        }

        addSource(description) {
            invalidate()
        }

        addSource(_extras) {
            invalidate()
        }
    }

    val isValid: LiveData<Boolean> = _isValid

    init {
        //set broadcast receiver as the default target
        targetBroadcastReceiver.value = true
    }

    fun setExtraName(uid: String, name: String) {
        _extras.value = _extras.value?.map {
            if (it.uid == uid && it.name != name) {
                return@map it.copy(name = name)
            }

            it
        }
    }

    fun setExtraValue(uid: String, value: String) {
        _extras.value = _extras.value?.map {
            if (it.uid == uid && it.value != value) {
                return@map it.copy(value = value)
            }

            it
        }
    }

    fun removeExtra(uid: String) {
        _extras.value = _extras.value?.toMutableList()?.apply {
            removeAll { it.uid == uid }
        }
    }

    fun addExtra(type: IntentExtraType) {
        val model = IntentExtraModel(type = type)

        _extras.value = _extras.value?.toMutableList()?.apply {
            add(model)
        }
    }

    fun setListItemModels(models: List<IntentExtraListItemModel>) {
        if (models.isEmpty()) {
            _extrasListItemModels.value = Empty()
            return
        }

        _extrasListItemModels.value = Data(models)
    }

    fun getTarget(): IntentTarget = when {
        targetActivity.value == true -> IntentTarget.ACTIVITY
        targetBroadcastReceiver.value == true -> IntentTarget.BROADCAST_RECEIVER
        targetService.value == true -> IntentTarget.SERVICE
        else -> throw Exception("No target selected.")
    }

    fun setActivity(activityInfo: ActivityInfo) {
        targetPackage.value = activityInfo.packageName
        targetClass.value = activityInfo.activityName
    }

    fun setFlags(flags: Int) {
        this.flags.value = flags.toString()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return IntentActionTypeViewModel() as T
        }
    }
}
