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
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class MessageListItemLiveDataBenchmark {

    // TODO: BenchmarkRule is broken unfortunately
    // @get:Rule
    // val benchmarkRule = BenchmarkRule()

    private val currentUser = randomUser()

    private fun simpleDateGroups(message: Message): String? {
        val day = Date(message.createdAt?.time ?: 0)
        return SimpleDateFormat("MM / dd").format(day)
    }

    private fun manyMessages(): MessageListItemLiveData {
        val messages = mutableListOf<Message>()

        for (i in (0..5)) {
            val user = randomUser()
            for (y in 0..50) {
                val message = randomMessage(user = user, createdAt = calendar(2020, 11, i % 28 + 1, 1, i, y))
                messages.add(message)
            }
        }
        val messagesLd: LiveData<List<Message>> = MutableLiveData(messages)
        // user 0 read till the end, user 1 read the first message, user 3 read is missing
        val read1 = ChannelUserRead(messages.first().user, messages.last().createdAt)
        val read2 = ChannelUserRead(messages[10].user, messages.first().createdAt)
        val reads: LiveData<List<ChannelUserRead>> = MutableLiveData(listOf(read1, read2))
        val typing: LiveData<List<User>> = MutableLiveData(listOf())

        return MessageListItemLiveData(currentUser, messagesLd, reads, typing, false, ::simpleDateGroups)
    }

    @Test
    fun `the most frequent change to the message list is typing changes`() {
        val messageLd = manyMessages()
        val items = messageLd.getOrAwaitValue().items

        val duration = measureTimeMillis {
            for (x in 0..50) {
                messageLd.typingChanged(listOf(User(id = x.toString())))
                messageLd.typingChanged(emptyList())
            }
        }
        println("changing typing information 100 times on a message list with ${items.size} items took $duration milliseconds")
        Truth.assertThat(duration).isLessThan(20)
    }

    @Test
    fun `the second most frequent change is read state changes`() {
        val messageLd = manyMessages()
        val items = messageLd.getOrAwaitValue().items

        val users = items.filterIsInstance<MessageListItem.MessageItem>().map { it.message.user }.distinct()
        val messages = items.filterIsInstance<MessageListItem.MessageItem>().map { it.message.createdAt }.takeLast(5)
        val reads = users.map { ChannelUserRead(it, messages.random()) }

        val duration = measureTimeMillis {
            for (x in 0..100) {
                messageLd.readsChanged(reads)
            }
        }
        println("changing read information 100 times on a message list with ${items.size} items took $duration milliseconds")
        Truth.assertThat(duration).isLessThan(40)
    }

    @Test
    fun `new messages dont happen as often`() {
        val messageLd = manyMessages()
        val items = messageLd.getOrAwaitValue().items

        val messages = items.filterIsInstance<MessageListItem.MessageItem>().map { it.message }

        val duration = measureTimeMillis {
            for (x in 0..100) {
                val newMessages = messages + randomMessage()
                messageLd.messagesChanged(newMessages)
            }
        }
        println("changing messages 100 times on a message list with ${items.size} items took $duration milliseconds")
        Truth.assertThat(duration).isLessThan(200)
    }
}
