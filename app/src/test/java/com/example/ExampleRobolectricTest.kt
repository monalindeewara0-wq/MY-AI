package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app_name string resource from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MY AI", appName)
    }

    @Test
    fun `insert and retrieve conversation in Room database`() = runBlocking {
        val convDao = db.conversationDao()
        val conv = ConversationEntity(title = "WWE Chat")
        val id = convDao.insertConversation(conv)

        val retrieved = convDao.getConversationById(id)
        assertNotNull(retrieved)
        assertEquals("WWE Chat", retrieved?.title)
    }

    @Test
    fun `insert message and retrieve message list in Room database`() = runBlocking {
        val convDao = db.conversationDao()
        val msgDao = db.messageDao()

        val convId = convDao.insertConversation(ConversationEntity(title = "GTA Chat"))
        val userMsg = MessageEntity(
            conversationId = convId,
            role = "user",
            content = "Mata GTA game ekak kiyanna"
        )
        msgDao.insertMessage(userMsg)

        val messages = msgDao.getMessagesList(convId)
        assertEquals(1, messages.size)
        assertEquals("Mata GTA game ekak kiyanna", messages[0].content)
        assertEquals("user", messages[0].role)
    }
}
