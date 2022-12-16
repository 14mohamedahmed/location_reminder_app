package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource

    private val reminder =
        ReminderDTO("reminder", "Description1", "Location1", 30.151351165, 29.351531, "1")

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setUpViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    // validate reminder list is not empty
    @Test
     fun loadReminders_validateListNotEmpty() = coroutineRule.runBlockingTest {
        fakeDataSource.saveReminder(reminder)
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.remindersList.value!!.isNotEmpty())
    }

    // show SnackBar when loading data failed
    @Test
    fun loadReminders_ShowSnackBarWhenError() {
        coroutineRule.pauseDispatcher()
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()
        coroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue()).isEqualTo("Error getting reminders")
    }

    // trying to loadReminders and check if loading value is changed or not
    @Test
    fun loadReminders_CheckDisplayLoading() {
        coroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        Truth.assertThat(remindersListViewModel.showLoading.getOrAwaitValue()).isTrue()
        coroutineRule.resumeDispatcher()
        Truth.assertThat(remindersListViewModel.showLoading.getOrAwaitValue()).isFalse()
    }
}