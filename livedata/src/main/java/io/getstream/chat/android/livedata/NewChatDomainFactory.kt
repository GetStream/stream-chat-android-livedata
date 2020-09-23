package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.NewChatClientImpl
import io.getstream.chat.android.client.api.ChatApi

class NewChatDomainFactory {

    /*
     * NewChatDomain always comes with the RoomStorageClient. This factory creates a configuration of NewChatClient
     * that can delegate requests to storage.
     */
    fun create() = NewChatDomain(NewChatClientImpl(ChatApi(), UserInitializer(), RoomStorageClient()))
}
