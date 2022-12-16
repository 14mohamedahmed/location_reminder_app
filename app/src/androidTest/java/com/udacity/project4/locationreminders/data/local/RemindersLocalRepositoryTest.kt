package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // GIVEN - reminderDTO
    private val reminder =
        ReminderDTO("reminder", "Description1", "Location1", 30.151351165, 29.351531, "1")


    @Before
    fun initDataSource() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        localDataSource = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDB() = database.close()

    // save reminder and get local data
    @Test
    fun saveReminderAndGetReminderById() = runBlocking {
        // GIVEN - save reminder in database
        localDataSource.saveReminder(reminder)

        // WHEN  - get reminder by id
        val result = localDataSource.getReminder(reminder.id)

        // THEN - check is same reminder is returned
        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`("reminder"))
        assertThat(result.data.description, `is`("Description1"))
        assertThat(result.data.id, `is`("1"))
        assertThat(result.data.location, `is`("Location1"))
        assertThat(result.data.latitude, `is`(30.151351165))
        assertThat(result.data.longitude, `is`(29.351531))
    }

    // save reminder and get all reminders
    @Test
    fun saveRemindersAndGetAllReminders() = runBlocking {
        // GIVEN - save reminder to local data source
        localDataSource.saveReminder(reminder)
        // WHEN  - get all reminders
        val res = localDataSource.getReminders()
        // THEN - check if reminder is success. then check list not equal to null and it's size is 1
        res as Result.Success
        assertThat(res.data.isNotEmpty(), `is`(true))
        assertThat(res.data.size, `is`(1))
    }

    // save reminder and delete it then check if is deleted
    @Test
    fun saveRemindersAndDeletesAllReminders() = runBlocking {
        // GIVEN - save reminder
        localDataSource.saveReminder(reminder)
        // WHEN - delete all reminders
        localDataSource.deleteAllReminders()
        // get all reminders
        val res = localDataSource.getReminders()
        // THEN - expected result success with no data in list
        res as Result.Success
        assertThat(res.data.isEmpty(), `is`(true))
        assertThat(res.data.size, `is`(0))
    }

    // If there are any items(reminders), we remove them all in this method and attempt to obtain a reminder using an invalid id.
    @Test
    fun getReminderAndReturnsError() = runBlocking {
        //GIVEN - delete all reminders
        localDataSource.deleteAllReminders()
        //WHEN - check if reminder.id is exist in reminders list
        val res = localDataSource.getReminder(reminder.id)
        res as Result.Error
        //THEN - Expected error because list is empty
        assertThat(res.message, `is`("Reminder not found!"))
    }

}