package io.getstream.chat.android.livedata.utils

/**
 * MessageListItemWrapper wraps a list of MessageListItem with a few extra fields.
 */
data class MessageListItemWrapper(
    var items: List<MessageListItem> = listOf(),
    var loadingMore: Boolean = false,
    val hasNewMessages: Boolean = false,
    val isTyping: Boolean = false,
    val isThread: Boolean = false
)
