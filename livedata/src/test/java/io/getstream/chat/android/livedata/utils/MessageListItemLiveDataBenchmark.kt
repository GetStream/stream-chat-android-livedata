package io.getstream.chat.android.livedata.utils

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.livedata.randomMessage
import io.getstream.chat.android.livedata.randomUser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(AndroidJUnit4::class)
class MessageListItemLiveDataBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val currentUser = randomUser()

    private fun simpleDateGroups(message: Message): String? {
        val day = Date(message.createdAt?.time ?: 0)
        return SimpleDateFormat("MM / dd").format(day)
    }

    private fun manyMessages(): MessageListItemLiveData {
        val messages = mutableListOf<Message>()
        val users = listOf(randomUser(), randomUser(), randomUser())

        for (i in (0..2)) {
            val user = users[i]
            for (y in 0..2) {
                val message = randomMessage(user = user, createdAt = calendar(2020, 11, i + 1, i + y))
                messages.add(message)
            }
        }
        val messagesLd: LiveData<List<Message>> = MutableLiveData(messages)
        // user 0 read till the end, user 1 read the first message, user 3 read is missing
        val read1 = ChannelUserRead(users[0], messages.last().createdAt)
        val read2 = ChannelUserRead(users[1], messages.first().createdAt)
        val reads: LiveData<List<ChannelUserRead>> = MutableLiveData(listOf(read1, read2))
        val typing: LiveData<List<User>> = MutableLiveData(listOf())

        return MessageListItemLiveData(currentUser, messagesLd, reads, typing, false, ::simpleDateGroups)
    }

    @Test
    fun twentyMillis() = benchmarkRule.measureRepeated {

        Thread.sleep(20)
    }
}
