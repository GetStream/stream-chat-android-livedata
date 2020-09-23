package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.NewChatClient

class NewChatDomain(private val chatClient: NewChatClient) : NewChatClient by chatClient

