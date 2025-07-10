package io.github.sds100.keymapper.base.promode

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ViewModelScoped
class ProModeSetupUseCaseImpl @Inject constructor(
    private val preferences: PreferenceRepository,
) : ProModeSetupUseCase {
    override val isWarningUnderstood: Flow<Boolean> =
        preferences.get(Keys.isProModeWarningUnderstood).map { it ?: false }

    override fun onUnderstoodWarning() {
        preferences.set(Keys.isProModeWarningUnderstood, true)
    }
}

interface ProModeSetupUseCase {
    val isWarningUnderstood: Flow<Boolean>
    fun onUnderstoodWarning()
}
