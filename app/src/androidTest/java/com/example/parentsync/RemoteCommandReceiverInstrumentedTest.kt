package com.example.parentsync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentsync.data.local.ParentSyncDatabase
import com.example.parentsync.data.model.RemoteCommandDto
import com.example.parentsync.service.RemoteCommandReceiver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteCommandReceiverInstrumentedTest {

    private lateinit var database: ParentSyncDatabase
    private lateinit var receiver: RemoteCommandReceiver

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = ParentSyncDatabase.getDatabase(context)
        receiver = RemoteCommandReceiver(context, database.appUsageLimitDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testReceiveBlockAppCommand() = runBlocking {
        val packageName = "com.example.testapp"
        val command = RemoteCommandDto(
            commandId = "cmd_101",
            commandType = "BLOCK_APP",
            packageName = packageName
        )
        receiver.receiveCommand(command)
        val limit = database.appUsageLimitDao().getLimit(packageName)
        assertTrue(limit?.isBlocked == true)
    }

    @Test
    fun testReceiveUpdateQuotaCommand() = runBlocking {
        val packageName = "com.example.testgame"
        val command = RemoteCommandDto(
            commandId = "cmd_102",
            commandType = "UPDATE_QUOTA",
            packageName = packageName,
            quotaMinutes = 30
        )
        receiver.receiveCommand(command)
        val limit = database.appUsageLimitDao().getLimit(packageName)
        assertEquals(30, limit?.dailyQuotaMinutes)
    }
}
