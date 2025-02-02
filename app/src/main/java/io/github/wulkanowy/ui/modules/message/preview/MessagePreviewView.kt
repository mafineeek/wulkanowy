package io.github.wulkanowy.ui.modules.message.preview

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.db.entities.MessageWithAttachment
import io.github.wulkanowy.ui.base.BaseView

interface MessagePreviewView : BaseView {

    val deleteMessageSuccessString: String

    val messageNoSubjectString: String

    val printHTML: String

    fun initView()

    fun setMessageWithAttachment(item: MessageWithAttachment)

    fun showProgress(show: Boolean)

    fun showContent(show: Boolean)

    fun showErrorView(show: Boolean)

    fun setErrorDetails(message: String)

    fun setErrorRetryCallback(callback: () -> Unit)

    fun showOptions(show: Boolean)

    fun setDeletedOptionsLabels()

    fun setNotDeletedOptionsLabels()

    fun openMessageReply(message: Message?)

    fun openMessageForward(message: Message?)

    fun shareText(text: String, subject: String)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun printDocument(html: String, jobName: String)

    fun popView()
}
