package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.api.models.QuerySort
import io.getstream.chat.android.client.utils.FilterObject
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.controller.QueryChannelsController
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import kotlinx.coroutines.launch

interface QueryChannels {
    /**
     * Queries offline storage and the API for channels matching the filter
     * Returns a queryChannelsController
     *
     * @param filter the filter object
     * @param sort how to sort the channels (default is last_message_at)
     * @param limit the number of channels to retrieve
     * @param messageLimit how many messages to retrieve per channel
     *
     * @return A call object with QueryChannelsController as the return type
     * @see io.getstream.chat.android.livedata.controller.QueryChannelsController
     * @see io.getstream.chat.android.client.utils.FilterObject
     * @see io.getstream.chat.android.client.api.models.QuerySort
     * @see <a href="https://getstream.io/chat/docs/query_channels/?language=kotlin">Filter syntax</a>
     */
    operator fun invoke(filter: FilterObject, sort: QuerySort, limit: Int = 30, messageLimit: Int = 10): Call2<QueryChannelsController>
}

class QueryChannelsImpl(var domainImpl: ChatDomainImpl) : QueryChannels {
    override operator fun invoke(filter: FilterObject, sort: QuerySort, limit: Int, messageLimit: Int): Call2<QueryChannelsController> {
        val queryChannelsControllerImpl = domainImpl.queryChannels(filter, sort)
        val queryChannelsController: QueryChannelsController = queryChannelsControllerImpl
        val runnable = suspend {
            if (limit > 0) {
                queryChannelsControllerImpl.scope.launch { queryChannelsControllerImpl.query(limit, messageLimit) }
            }
            Result(queryChannelsController, null)
        }
        return CallImpl2(
            runnable,
            queryChannelsControllerImpl.scope, true
        )
    }
}
