package io.getstream.chat.android.livedata.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User

/**
 * It's common for messaging UIs to interleave and group messages
 *
 * - subsequent messages from the same user are typically grouped
 * - read state is typically shown
 * - date separators are common
 * - typing indicators are typically shown at the bottom
 *
 * The class merges the livedata objects for messages, read state and typing
 *
 * - If typing indicators should be used
 * - How/if to use date separators
 *
 * Example date separator
 *
 * {
 *  val day = Date(message.createdAt?.time ?: 0)
 *  SimpleDateFormat("MM / dd").format(day)
 * }
 */
class MessageListItemLiveData(messages: LiveData<List<Message>>, reads: LiveData<List<ChannelUserRead>>, typing: LiveData<List<User>>? = null, dateSeparator: ((m: Message) -> String?)? = null) {
    val mediator = MediatorLiveData<List<MessageListItem>>()
    init {
        mediator.addSource(messages) { value ->
            messagesChanged(messages)
        }
        mediator.addSource(reads) { value ->
            readsChanged(value)
        }
        if (typing != null) {
            mediator.addSource(typing) { value ->
                typingChanged(value)
            }
        }
    }

    private fun messagesChanged(messages: LiveData<List<Message>>) {
    }

    private fun readsChanged(value: List<ChannelUserRead>) {
    }

    private fun typingChanged(value: List<User>) {
    }
}
