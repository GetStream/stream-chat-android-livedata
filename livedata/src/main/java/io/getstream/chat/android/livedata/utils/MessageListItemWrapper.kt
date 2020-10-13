package io.getstream.chat.android.livedata.utils

data class MessageListItemWrapper(
    var loadingMore: Boolean = false,
    val hasNewMessages: Boolean = false,
    var items: List<MessageListItem> = listOf(),
    val isTyping: Boolean = false,
    val isThread: Boolean = false
)
