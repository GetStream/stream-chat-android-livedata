package io.getstream.chat.android.livedata.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.livedata.extensions.getCreatedAtOrThrow
import java.util.Date

/**
 * It's common for messaging UIs to interleave and group messages
 *
 * - subsequent messages from the same user are typically grouped
 * - read state is typically shown (either as part of the message or separately)
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
 * - Read state matches to the right message
 * - Leverages MediatorLiveData to improve handling of null values
 * - Efficient algorithm for updating read state
 * - Makes the MessageListItem immutable to prevent future bugs
 *
 */
class MessageListItemLiveData(
    val currentUser: User,
    val messagesLiveData: LiveData<List<Message>>,
    val readsLiveData: LiveData<List<ChannelUserRead>>,
    val typingLiveData: LiveData<List<User>>? = null,
    val isThread: Boolean = false,
    val dateSeparator: ((m: Message) -> String?)? = null
) : MediatorLiveData<MessageListItemWrapper>() {

    private var hasNewMessages: Boolean = false
    private var messageItemsBase = listOf<MessageListItem>()
    private var messageItemsWithReads = listOf<MessageListItem>()
    private var typingUsers = listOf<User>()
    private var typingItems = listOf<MessageListItem>()

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

        hasNewMessages = false
        val newlastMessageID: String = messages[messages.size - 1].id
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
            val message: Message = messages[i]
            var nextMessage: Message? = null
            if (i + 1 <= topIndex) {
                nextMessage = messages[i + 1]
            }

            if (i == 1 && isThread) {

                entities.add(MessageListItem.ThreadSeparatorItem(message.getCreatedAtOrThrow()))
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

            val messageListItem: MessageListItem = MessageListItem.MessageItem(message, positions, isMine = message.user.id == currentUser.id)
            entities.add(messageListItem)
            previousMessage = message
        }
        return entities.toList()
    }

    /**
     * Reads changing is the second most common change on the message item list
     * Since the most common scenario is that someone read to the end, we start by matching the end of the list
     * We also sort the read state for easier merging of the lists
     */
    private fun addReads(messages: List<MessageListItem>, reads: List<ChannelUserRead>?): List<MessageListItem> {
        if (reads == null || reads.isEmpty() || messages.isEmpty()) return messages

        val sortedReads = reads.sortedBy { it.lastRead }.toMutableList()
        val messagesCopy = messages.toMutableList()

        // start at the end, optimized for the most common scenario that most people are watching the chat
        for ((i, messageItem) in messages.reversed().withIndex()) {
            if (messageItem is MessageListItem.MessageItem) {
                val messageItem = messageItem as MessageListItem.MessageItem
                messageItem.message.createdAt?.let {
                    while (sortedReads.isNotEmpty()) {
                        // use the list of sorted reads
                        val last = sortedReads.last()
                        if (it.before(last.lastRead) || it == last.lastRead) {
                            // we got a match
                            sortedReads.removeLast()
                            val reversedIndex = messages.size - i - 1
                            val messageItemCopy = messagesCopy[reversedIndex] as MessageListItem.MessageItem
                            val readBy = listOf(last) + messageItemCopy.messageReadBy
                            val updatedMessageItem = messageItem.copy(messageReadBy = readBy)
                            // update the message in the message copy
                            messagesCopy[reversedIndex] = updatedMessageItem
                        } else {
                            // search further in the past for matches
                            break
                        }
                    }
                }
            }
        }

        return messagesCopy
    }

    internal fun wrapMessages(items: List<MessageListItem>): MessageListItemWrapper {
        return MessageListItemWrapper(items = items, isThread = isThread, isTyping = typingUsers.isNotEmpty(), hasNewMessages = hasNewMessages)
    }

    internal fun messagesChanged(messages: List<Message>): List<MessageListItem> {
        messageItemsBase = groupMessages()
        messageItemsWithReads = addReads(messageItemsBase, readsLiveData.value)
        val out = messageItemsWithReads + typingItems
        val wrapped = wrapMessages(out)
        value = wrapped.copy(hasNewMessages = hasNewMessages)
        return out
    }

    internal fun readsChanged(reads: List<ChannelUserRead>): List<MessageListItem> {
        messageItemsWithReads = addReads(messageItemsBase, readsLiveData.value)
        val out = messageItemsWithReads + typingItems
        value = wrapMessages(out)
        return out
    }

    internal fun usersAsTypingItems(): List<MessageListItem> {
        return if (typingUsers.isNotEmpty()) {
            listOf(MessageListItem.TypingItem(typingUsers))
        } else {
            emptyList()
        }
    }

    /**
     * Typing changes are the most common changes on the message list
     * Note how they don't recompute the message list, but only add to the end
     */
    internal fun typingChanged(users: List<User>): List<MessageListItem> {
        val newTypingUsers = users.filter { it.id != currentUser.id }

        return if (newTypingUsers != typingUsers) {
            typingUsers = newTypingUsers
            typingItems = usersAsTypingItems()
            val out = messageItemsWithReads + typingItems
            value = wrapMessages(out)
            out
        } else {
            messageItemsWithReads + typingItems
        }
    }
}
