package io.getstream.chat.android.livedata

import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Member
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import java.util.Date

/**
 * A class that only stores the channel data and not all the other channel state
 * Using this prevents code bugs and issues caused by confusing the channel data vs the full channel object
 */
data class ChannelData(var type: String, var channelId: String) {
    var cid: String = "%s:%s".format(type, channelId)

    /** created by user */
    var createdBy: User = User()

    /** if the channel is frozen or not (new messages wont be allowed) */
    var frozen: Boolean = false

    /** when the channel was created */
    var createdAt: Date? = null
    /** when the channel was updated */
    var updatedAt: Date? = null
    /** when the channel was deleted */
    var deletedAt: Date? = null
    /** all the custom data provided for this channel */
    var extraData = mutableMapOf<String, Any>()

    /** create a ChannelData object from a Channel object */
    constructor(c: Channel) : this(c.type, c.id) {
        frozen = c.frozen
        createdAt = c.createdAt
        updatedAt = c.updatedAt
        deletedAt = c.deletedAt
        extraData = c.extraData

        createdBy = c.createdBy
    }

    /** convert a channelEntity into a channel object */
    fun toChannel(messages: List<Message>, members: List<Member>, reads: List<ChannelUserRead>, watchers: List<User>, watcherCount: Int): Channel {
        val c = Channel()
        c.type = type
        c.id = channelId
        c.cid = cid
        c.frozen = frozen
        c.createdAt = createdAt
        c.updatedAt = updatedAt
        c.deletedAt = deletedAt
        c.extraData = extraData
        c.lastMessageAt = messages.lastOrNull()?.let { it.createdAt ?: it.createdLocallyAt }
        c.createdBy = createdBy

        c.messages = messages
        c.members = members

        c.watchers = watchers
        c.watcherCount = watcherCount

        c.read = reads

        return c
    }
}
