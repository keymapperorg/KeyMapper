package io.github.sds100.keymapper.base.bugreport

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.files.IFile
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class BugReportViewModel @Inject constructor(private val compiler: BugReportCompiler) :
    ViewModel() {

    val state: StateFlow<BugReportState> = compiler.state

    suspend fun compile(): KMResult<IFile> = compiler.compile()

    fun getShareUri(file: IFile): Uri = compiler.getShareUri(file)
}
