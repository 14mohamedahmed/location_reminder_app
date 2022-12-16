package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    // GIVEN - reminderDTO
    private val reminder =
        ReminderDTO("reminder", "Description1", "Location1", 30.151351165, 29.351531, "1")

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runBlockingTest {
        // GIVEN - save reminder
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val result = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(result as ReminderDTO, notNullValue())

        assertThat(result, notNullValue())
        assertThat(result.id, `is`(reminder.id))
        assertThat(result.title, `is`(reminder.title))
        assertThat(result.description, `is`(reminder.description))
        assertThat(result.location, `is`(reminder.location))
        assertThat(result.latitude, `is`(reminder.latitude))
        assertThat(result.longitude, `is`(reminder.longitude))

    }

    // get all reminder saved in database
    @Test
    fun getAllRemindersFromDb() = runBlockingTest {
        // GIVEN - save reminder
        database.reminderDao().saveReminder(reminder)
        // WHEN - Get all reminders.
        val remindersList = database.reminderDao().getReminders()
        // THEN - check if reminderList not empty
        assertThat(remindersList, `is`(notNullValue()))
    }

    // delete all reminders from database
    @Test
    fun insertReminders_deleteAllReminders() = runBlockingTest {
        // GIVEN - save reminder the delete it
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()
        // WHEN - Get all reminders.
        val remindersList = database.reminderDao().getReminders()
        // check if reminders is empty
        assertThat(remindersList, `is`(emptyList()))
    }


//    TODO: Add testing implementation to the RemindersLocalRepository.kt

}