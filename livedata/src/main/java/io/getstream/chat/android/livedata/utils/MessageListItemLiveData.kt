package io.getstream.chat.android.livedata.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import java.util.Date

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
 *
 * It improves upon the previous Java code in the sample app in a few ways
 * - Kotlin
 * - Typing indicators can be turned on/off
 * - Date separators can be turned off and are configurable
 * - Leverages MediatorLiveData to improve handling of null values
 * - Efficient algorithm for updating read state
 * - Makes the MessageListItem immutable to prevent future bugs
 *
 * TODO:
 * - How do we handle threads. I don't like the current setup
 *
 */
data class MessageListItemLiveData(val currentUser: User, val messagesLiveData: LiveData<List<Message>>, val readsLiveData: LiveData<List<ChannelUserRead>>, val typingLiveData: LiveData<List<User>>? = null, val dateSeparator: ((m: Message) -> String?)? = null) : MediatorLiveData<List<MessageListItem>>() {

    private var messageItemsBase = listOf<MessageListItem>()
    private var messageItemsWithReads = listOf<MessageListItem>()

    private var lastMessageID = ""

    init {
        addSource(messagesLiveData) { value ->
            messagesChanged(value)
        }
        addSource(readsLiveData) { value ->
            readsChanged(value)
        }
        if (typingLiveData != null) {
            addSource(typingLiveData) { value ->
                typingChanged(value)
            }
        }
    }

    private fun groupMessages(): List<MessageListItem> {
        val messages = messagesLiveData.value

        if (messages == null || messages.isEmpty()) return emptyList()

        var hasNewMessages = false
        val newlastMessageID: String = messages.get(messages.size - 1).id
        if (newlastMessageID != lastMessageID) {
            hasNewMessages = true
        }
        lastMessageID = newlastMessageID

        val entities = mutableListOf<MessageListItem>()
        val now = Date()
        var previousMessage: Message? = null
        val size: Int = messages.size
        val topIndex = Math.max(0, size - 1)

        for (i in 0 until size) {
            val message: Message = messages.get(i)
            var nextMessage: Message? = null
            if (i + 1 <= topIndex) {
                nextMessage = messages[i + 1]
            }

            // determine the position (top, middle, bottom)
            val user = message.user
            val positions = mutableListOf<MessageListItem.Position>()
            if (previousMessage == null || previousMessage.user != user) {
                positions.add(MessageListItem.Position.Top)
            }
            if (nextMessage == null || nextMessage.user != user) {
                positions.add(MessageListItem.Position.Bottom)
            }
            if (previousMessage != null && nextMessage != null) {
                if (previousMessage.user == user && nextMessage.user == user) {
                    positions.add(MessageListItem.Position.Middle)
                }
            }
            // date separators
            if (dateSeparator != null) {
                previousMessage?.let {
                    val previousKey = dateSeparator?.let { it(previousMessage!!) }
                    val currentKey = dateSeparator?.let { it(message) }
                    if (previousKey != currentKey) {
                        entities.add(MessageListItem.DateSeparatorItem(message.createdAt ?: now))
                    }
                }
                if (previousMessage == null) {
                    entities.add(MessageListItem.DateSeparatorItem(message.createdAt ?: now))
                }
            }

            val messageListItem: MessageListItem = MessageListItem.MessageItem(message, positions)
            entities.add(messageListItem)
            previousMessage = message
        }
        return entities.toList()
    }

    private fun addReads(messages: List<MessageListItem>, reads: List<ChannelUserRead>?): List<MessageListItem> {
        if (reads == null || reads.isEmpty()) return messages

        val sortedReads = reads.sortedByDescending { it.lastRead }.toMutableList()
        val messagesCopy = messages.toMutableList()

        // start at the end
        for (messageItem in messages.reversed()) {
            var index = messages.size - 1
            if (messageItem is MessageListItem.MessageItem) {
                val messageItem = messageItem as MessageListItem.MessageItem
                messageItem.message.createdAt?.let {
                    while (sortedReads.isNotEmpty()) {
                        // use the list of sorted reads
                        val last = sortedReads.last()
                        if (it.before(last.lastRead)) {
                            // we got a match
                            sortedReads.removeLast()
                            val messageItemCopy = messagesCopy[index] as MessageListItem.MessageItem
                            val readBy = listOf(last) + messageItemCopy.messageReadBy
                            val updatedMessageItem = messageItem.copy(messageReadBy = readBy)
                            // update the message in the message copy
                            messagesCopy[index] = updatedMessageItem
                        } else {
                            // search further in the past for matches
                            break
                        }
                    }
                }
            }
            index--
        }

        return messagesCopy
    }

    private fun messagesChanged(messages: List<Message>) {
        messageItemsBase = groupMessages()
        messageItemsWithReads = addReads(messageItemsBase, readsLiveData.value)
        value = messageItemsWithReads
    }

    private fun readsChanged(reads: List<ChannelUserRead>) {
        messageItemsWithReads = addReads(messageItemsBase, readsLiveData.value)
        value = messageItemsWithReads
    }

    private fun typingChanged(users: List<User>) {
        var output = messageItemsWithReads
        val typingUsers = users.filter { it.id != currentUser.id }
        if (typingUsers.isNotEmpty()) {
            output = output + MessageListItem.TypingItem(typingUsers)
        }
        value = output
    }
}
