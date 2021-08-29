package io.github.sds100.keymapper.system.share

/**
 * Created by sds100 on 29/08/2021.
 */
data class EmailModel(val message: String? = null,
                      /**
                       * Uri to the attachments.
                       */
                      val attachmentUri: String? = null)
