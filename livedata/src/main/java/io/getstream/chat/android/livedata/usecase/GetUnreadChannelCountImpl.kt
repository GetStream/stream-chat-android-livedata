package io.getstream.chat.android.livedata.usecase

import androidx.lifecycle.LiveData
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2

interface GetUnreadChannelCount {
    /**
     * Returns the number of channels with unread messages for the given user.
     * You might also be interested in GetTotalUnreadCount
     * Or ChannelController.unreadCount
     *
     * @return A call object with LiveData<Int> as the return type
     * @see io.getstream.chat.android.livedata.usecase.GetTotalUnreadCount
     * @see io.getstream.chat.android.livedata.controller.ChannelController.unreadCount
     */
    operator fun invoke(): Call2<LiveData<Int>>
}

class GetUnreadChannelCountImpl(var domainImpl: ChatDomainImpl) : GetUnreadChannelCount {
    override operator fun invoke(): Call2<LiveData<Int>> {
        val runnable = suspend {
            Result(domainImpl.channelUnreadCount, null)
        }
        return CallImpl2<LiveData<Int>>(
            runnable,
            domainImpl.scope,
            true
        )
    }
}
