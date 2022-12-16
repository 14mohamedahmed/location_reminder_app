package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: FakeDataSource

    private val reminder =
        ReminderDataItem("reminder", "Description1", "Location1", 30.151351165, 29.351531, "1")

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
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    // test reminder when title is empty then check if snackbar message is "please enter title"
    @Test
    fun validateEnteredData_EmptyTitleShowSnackBar() {
        reminder.title = ""
        Truth.assertThat(saveReminderViewModel.validateEnteredData(reminder)).isFalse()
        Truth.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue())
            .isEqualTo(R.string.err_enter_title)
    }

    // test reminder when location is empty then check if snackbar message is "please enter location"
    @Test
    fun validateEnteredData_EmptyLocationShowSnackBar() {
        reminder.location = ""
        Truth.assertThat(saveReminderViewModel.validateEnteredData(reminder)).isFalse()
        Truth.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue())
            .isEqualTo(R.string.err_select_location)
    }

    // test reminder and check if valid save reminder then update show loading
    @Test
    fun validateEnteredData_SaveDataWhenValid() {
        coroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(reminder)
        Truth.assertThat(saveReminderViewModel.showLoading.getOrAwaitValue()).isTrue()
        coroutineRule.resumeDispatcher()
        Truth.assertThat(saveReminderViewModel.showLoading.getOrAwaitValue()).isFalse()

    }

}