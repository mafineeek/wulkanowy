package io.github.wulkanowy.data.repositories.message

import io.github.wulkanowy.data.db.dao.MessageAttachmentDao
import io.github.wulkanowy.data.db.dao.MessagesDao
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.db.entities.MessageAttachment
import io.github.wulkanowy.data.db.entities.MessageWithAttachment
import io.github.wulkanowy.data.db.entities.Student
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageLocal @Inject constructor(
    private val messagesDb: MessagesDao,
    private val messageAttachmentDao: MessageAttachmentDao
) {

    suspend fun saveMessages(messages: List<Message>) {
        messagesDb.insertAll(messages)
    }

    suspend fun updateMessages(messages: List<Message>) {
        messagesDb.updateAll(messages)
    }

    suspend fun deleteMessages(messages: List<Message>) {
        messagesDb.deleteAll(messages)
    }

    fun getMessageWithAttachment(student: Student, message: Message): Flow<MessageWithAttachment?> {
        return messagesDb.loadMessageWithAttachment(student.id.toInt(), message.messageId)
    }

    suspend fun saveMessageAttachments(attachments: List<MessageAttachment>) {
        messageAttachmentDao.insertAttachments(attachments)
    }

    fun getMessages(student: Student, folder: MessageFolder): Flow<List<Message>> {
        return messagesDb.loadAll(student.id.toInt(), folder.id)
    }
}
