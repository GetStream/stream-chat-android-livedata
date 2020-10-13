package io.getstream.chat.android.livedata.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.livedata.randomMessage
import io.getstream.chat.android.livedata.randomUser
import io.getstream.chat.android.livedata.utils.MessageListItem.TypingItem
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date

/**
 * TODO:
 * - validate how the initial data flow works with .observe
 * - complete testing
 * - see if we need the wrapper
 */

@RunWith(AndroidJUnit4::class)
class MessageListItemLiveDataTest {

    fun emptyMessages(): MessageListItemLiveData {
        val message = randomMessage()
        val user = randomUser()
        val messages: LiveData<List<Message>> = MutableLiveData(listOf())
        val read = ChannelUserRead(user, Date())
        val reads: LiveData<List<ChannelUserRead>> = MutableLiveData(listOf())
        val typing: LiveData<List<User>> = MutableLiveData(listOf())

        return MessageListItemLiveData(user, messages, reads, typing)
    }

    fun oneMessage(): MessageListItemLiveData {
        val message = randomMessage()
        val user = randomUser()
        val messages: LiveData<List<Message>> = MutableLiveData(listOf(message))
        val read = ChannelUserRead(user, Date())
        val reads: LiveData<List<ChannelUserRead>> = MutableLiveData(listOf())
        val typing: LiveData<List<User>> = MutableLiveData(listOf())

        return MessageListItemLiveData(user, messages, reads, typing)
    }

    fun manyMessages(): MessageListItemLiveData {
        val messages = mutableListOf<Message>()
        val currentUser = randomUser()
        val users = listOf(randomUser(), randomUser(), randomUser())

        for ((i, x) in (0..2).withIndex()) {
            val user = users[i]
            for (y in 0..2) {
                val message = randomMessage(user = user, createdAt = calendar(2020, 11, i + 1))
                messages.add(message)
            }
        }
        val messagesLd: LiveData<List<Message>> = MutableLiveData(messages)
        // user 0 read till the end, user 1 read the first message, user 3 read is missing
        val read1 = ChannelUserRead(users[0], messages.last().createdAt)
        val read2 = ChannelUserRead(users[1], messages.first().createdAt)
        val reads: LiveData<List<ChannelUserRead>> = MutableLiveData(listOf(read1, read2))
        val typing: LiveData<List<User>> = MutableLiveData(listOf())

        return MessageListItemLiveData(currentUser, messagesLd, reads, typing) {
            val day = Date(it.createdAt?.time ?: 0)
            SimpleDateFormat("MM / dd").format(day)
        }
    }

    // livedata testing
    @Test
    fun `Observe should trigger a recompute`() {
        val many = oneMessage()
        val items = many.getOrAwaitValue()
        Truth.assertThat(items.size).isEqualTo(2)
        val empty = emptyMessages()
        val items2 = empty.getOrAwaitValue()
        Truth.assertThat(items2.size).isEqualTo(0)
    }

    // test typing indicator logic:
    @Test
    fun `Should return an empty list`() {
        val messageListItemLd = emptyMessages()
        val items = messageListItemLd.typingChanged(emptyList())
        Truth.assertThat(items).isEmpty()
    }

    @Test
    fun `Should exclude the current user`() {
        val messageListItemLd = emptyMessages()
        val typing = listOf(messageListItemLd.currentUser)
        val items = messageListItemLd.typingChanged(typing)
        Truth.assertThat(items).isEmpty()
    }

    @Test
    fun `Should return only the typing indicator`() {
        val messageListItemLd = emptyMessages()
        val items = messageListItemLd.typingChanged(listOf(randomUser()))
        Truth.assertThat(items.size).isEqualTo(1)
        Truth.assertThat(items.last()).isInstanceOf(TypingItem::class.java)
    }

    @Test
    fun `Should return messages with a typing indicator`() {
        val messageListItemLd = oneMessage()
        messageListItemLd.messagesChanged(messageListItemLd.messagesLiveData.value!!)
        val items = messageListItemLd.typingChanged(listOf(randomUser()))
        Truth.assertThat(items.size).isEqualTo(3)
        Truth.assertThat(items.first()).isInstanceOf(MessageListItem.DateSeparatorItem::class.java)
        Truth.assertThat(items[1]).isInstanceOf(MessageListItem.MessageItem::class.java)
        Truth.assertThat(items.last()).isInstanceOf(TypingItem::class.java)
    }

    // test how we merge read state
    @Test
    fun `Last message should contain the read state`() {
        val messageListItemLd = manyMessages()
        val items = messageListItemLd.getOrAwaitValue()
        val lastMessage = items.last() as MessageListItem.MessageItem
        Truth.assertThat(lastMessage.messageReadBy).isNotEmpty()
    }

    @Test
    fun `First message should contain the read state`() {
        val messageListItemLd = manyMessages()
        val items = messageListItemLd.getOrAwaitValue()
        val lastMessage = items.first() as MessageListItem.MessageItem
        Truth.assertThat(lastMessage.messageReadBy).isNotEmpty()
    }

    // test message grouping
    @Test
    fun `There should be 3 messages with a position Top`() {
        val messageListItemLd = manyMessages()
        val topMessages = mutableListOf<MessageListItem.Position>()
        val items = messageListItemLd.getOrAwaitValue()
        for (item in items) {
            if (item is MessageListItem.MessageItem) {
                val messageItem = item as MessageListItem.MessageItem
                topMessages.addAll(messageItem.positions)
            }
        }
        // there are 3 users, so we should have 3 top sections
        val correctPositions = listOf(MessageListItem.Position.Top, MessageListItem.Position.Middle, MessageListItem.Position.Bottom)
        Truth.assertThat(topMessages).isEqualTo(correctPositions + correctPositions + correctPositions)
    }

    // test data separators
    @Test
    fun `There should be 3 date separators`() {
        val messageListItemLd = manyMessages()
        val separators = mutableListOf<MessageListItem.DateSeparatorItem>()
        val items = messageListItemLd.getOrAwaitValue()
        for (item in items) {
            if (item is MessageListItem.DateSeparatorItem) {
                separators.add(item)
            }
        }
        Truth.assertThat(separators.size).isEqualTo(3)
    }
}
