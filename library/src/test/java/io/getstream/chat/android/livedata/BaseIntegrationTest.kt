package io.getstream.chat.android.livedata

import org.junit.After
import org.junit.Before

open class BaseIntegrationTest: BaseDomainTest() {

    @Before
    override fun setup() {
        client = createClient()
        setupChatDomain(client, true)
    }

    @After
    override fun tearDown() {
        chatDomain.disconnect()
        db.close()
    }
}