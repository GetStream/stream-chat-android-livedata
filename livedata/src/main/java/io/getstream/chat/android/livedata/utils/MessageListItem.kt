package io.getstream.chat.android.livedata.utils

import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import java.util.Date
import java.util.zip.CRC32
import java.util.zip.Checksum

sealed class MessageListItem {

    fun getStableId(): Long {
        val checksum: Checksum = CRC32()
        val plaintext = when (this) {
            is TypingItem -> "id_typing"
            is ThreadSeparatorItem -> "id_thread_separator"
            is MessageItem -> message.id
            is DateSeparatorItem -> date.toString()
        }
        checksum.update(plaintext.toByteArray(), 0, plaintext.toByteArray().size)
        return checksum.value
    }

    data class DateSeparatorItem @JvmOverloads constructor(
        val date: Date,
    ) : MessageListItem()

    data class MessageItem @JvmOverloads constructor(
        val message: Message,
        val positions: List<Position> = listOf(),
        val isMine: Boolean = false,
        val messageReadBy: List<ChannelUserRead> = listOf()
    ) : MessageListItem()

    data class TypingItem @JvmOverloads constructor(
        val users: List<User>,
    ) : MessageListItem()

    data class ThreadSeparatorItem @JvmOverloads constructor(
        val date: Date = Date(),
    ) : MessageListItem()

    sealed class Position {
        object Top : Position()
        object Middle : Position()
        object Bottom : Position()
    }
}
