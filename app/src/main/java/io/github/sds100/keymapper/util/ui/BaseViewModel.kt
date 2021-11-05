package io.github.sds100.keymapper.util.ui

import androidx.lifecycle.ViewModel

/**
 * Created by sds100 on 05/11/2021.
 */
abstract class BaseViewModel(
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() 