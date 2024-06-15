package io.github.sds100.keymapper.reportbug

import io.github.sds100.keymapper.system.files.FileUtils

/**
 * Created by sds100 on 05/07/2021.
 */
object ReportBugUtils {
    fun createReportFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "key_mapper_report_$formattedDate.zip"
    }
}
