package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.NewChatClientImpl
import io.getstream.chat.android.client.api.ChatApi

object RoomStorageChatClientFactory {

    fun create() = NewChatClientImpl(ChatApi(), UserInitializer(), RoomStorageClient())
}
