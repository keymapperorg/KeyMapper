package com.example.architecturetest.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.architecturetest.data.KeymapRepository

/**
 * Created by sds100 on 26/01/2020.
 */

class NewKeymapViewModel internal constructor(
        repository: KeymapRepository
) : ConfigKeymapViewModel(repository) {
    class Factory(private val mRepository: KeymapRepository) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
                NewKeymapViewModel(mRepository) as T
    }
}