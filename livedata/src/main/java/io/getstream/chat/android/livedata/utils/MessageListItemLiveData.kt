package io.getstream.chat.android.livedata.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.livedata.extensions.getCreatedAtOrThrow

/**
 * It's common for messaging UIs to interleave and group messages
 *
 * - subsequent messages from the same user are typically grouped
 * - read state is typically shown (either as part of the message or separately)
 * - date separators are common
 * - typing indicators are typically shown at the bottom
 *
 * The MessageListItemLiveData class merges the livedata objects for messages, read state and typing
 *
 * It improves upon the previous Java code in the sample app in a few ways
 * - Kotlin
 * - Typing indicators can be turned on/off
 * - Date separators can be turned off and are configurable
 * - Read state matches to the right message
 * - Leverages MediatorLiveData to improve handling of null values
 * - Efficient algorithm for updating read state
 * - Efficient code for updating typing state
 * - Makes the MessageListItem immutable to prevent future bugs
 *
 *
 * @param currentUser the user who is currently authenticated
 * @param messageLd a livedata object with the messages
 * @param readsLd a livedata object with the read state per user
 * @param typingLd a livedata object with the users who are currently typing
 * @param isThread if we are in a thread or not. if in a thread we add a threadSeperator in position 1 of the item list
 * @param dateSeparator function to compare previous and current message and return if we should insert a date separator
 *
 * Here's an example:
 *
 * MessageListItemLiveData(currentUser, messagesLd, readsLd, typingLd, false) {
 *   return if (previous==null) {
 *       true
 *   } else {
 *       (message.getCreatedAtOrThrow().time - previous.getCreatedAtOrThrow().time) > (60 * 60 * 3)
 *   }
 * }
 *
 */
class MessageListItemLiveData(
    val currentUser: User,
    val messagesLd: LiveData<List<Message>>,
    val readsLd: LiveData<List<ChannelUserRead>>,
    val typingLd: LiveData<List<User>>? = null,
    val isThread: Boolean = false,
    val dateSeparator: ((previous: Message?, current: Message) -> Boolean)? = null
) : MediatorLiveData<MessageListItemWrapper>() {

    private var hasNewMessages: Boolean = false
    private var messageItemsBase = listOf<MessageListItem>()
    private var messageItemsWithReads = listOf<MessageListItem>()
    private var typingUsers = listOf<User>()
    private var typingItems = listOf<MessageListItem>()

    private var lastMessageID = ""

    init {
        addSource(messagesLd) { value ->
            messagesChanged(value)
        }
        addSource(readsLd) { value ->
            readsChanged(value)
        }
        if (typingLd != null) {
            addSource(typingLd) { value ->
                typingChanged(value)
            }
        }
    }

    internal fun messagesChanged(messages: List<Message>): List<MessageListItem> {
        messageItemsBase = groupMessages()
        messageItemsWithReads = addReads(messageItemsBase, readsLd.value)
        val out = messageItemsWithReads + typingItems
        val wrapped = wrapMessages(out)
        value = wrapped.copy(hasNewMessages = hasNewMessages)
        return out
    }

    internal fun readsChanged(reads: List<ChannelUserRead>): List<MessageListItem> {
        messageItemsWithReads = addReads(messageItemsBase, readsLd.value)
        val out = messageItemsWithReads + typingItems
        value = wrapMessages(out)
        return out
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

    /**
     * We could speed this up further in the case of a new message by only recomputing the last 2 items
     * It's fast enough though
     */
    private fun groupMessages(): List<MessageListItem> {
        val messages = messagesLd.value

        if (messages == null || messages.isEmpty()) return emptyList()

        hasNewMessages = false
        val newLastMessageId: String = messages[messages.size - 1].id
        if (newLastMessageId != lastMessageID) {
            hasNewMessages = true
        }
        lastMessageID = newLastMessageId

        val items = mutableListOf<MessageListItem>()
        var previousMessage: Message? = null
        val topIndex = 0.coerceAtLeast(messages.size - 1)

        for ((i, message) in messages.withIndex()) {
            var nextMessage: Message? = null
            if (i + 1 <= topIndex) {
                nextMessage = messages[i + 1]
            }

            // thread separator
            if (i == 1 && isThread) {
                items.add(MessageListItem.ThreadSeparatorItem(message.getCreatedAtOrThrow()))
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
            dateSeparator?.let {
                if (it(previousMessage, message)) {
                    items.add(MessageListItem.DateSeparatorItem(message.getCreatedAtOrThrow()))
                }
            }

            items.add(MessageListItem.MessageItem(message, positions, isMine = message.user.id == currentUser.id))
            previousMessage = message
        }
        return items.toList()
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

    private fun wrapMessages(items: List<MessageListItem>): MessageListItemWrapper {
        return MessageListItemWrapper(items = items, isThread = isThread, isTyping = typingUsers.isNotEmpty(), hasNewMessages = hasNewMessages)
    }

    private fun usersAsTypingItems(): List<MessageListItem> {
        return if (typingUsers.isNotEmpty()) {
            listOf(MessageListItem.TypingItem(typingUsers))
        } else {
            emptyList()
        }
    }
}
