package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.StorageClient
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.Message

/*
* With this class the client can use Room for storage. If the user desire to user prefer another database,
* another implementation can be used.
*/

class RoomStorageClient : StorageClient {

    override fun storeChannelState(channel: Channel) {
        //Some storage here!
    }

    override fun storeLastMessage(channelType: String, channelId: String, message: Message) {
        //Some storage here!
    }
}
