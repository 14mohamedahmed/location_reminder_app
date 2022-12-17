package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    //register espresso  before call test functions
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    // display snackbar error for reminder title
    @Test
    fun saveReminderScreen_showSnackBarTitleError() {
        // start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // navigate to save reminder page
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // click on save reminder => expected validation error in title
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())

        val snackBarMessage = appContext.getString(R.string.err_enter_title)
        // check that snackBar txt is displayed
        Espresso.onView(ViewMatchers.withText(snackBarMessage))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // close activity
        activityScenario.close()
    }

    // display snackbar error for reminder location
    @Test
    fun saveReminderScreen_showSnackBarLocationError() {
        // start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // navigate to save reminder page
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // type text in title
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Reminder Title"))
        // close keyboard after type title text
        Espresso.closeSoftKeyboard()
        // click on save reminder => expected validation error in location
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())

        val snackBarMessage = appContext.getString(R.string.err_select_location)
        // check that snackBar txt is displayed
        Espresso.onView(ViewMatchers.withText(snackBarMessage))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // close activity
        activityScenario.close()
    }

    // display snackbar error for reminder location
    @Test
    fun saveReminderScreen_CheckDataSubmittedSuccessfully() {
        // start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // navigate to save reminder page
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // type reminder data
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Reminder Title"))
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("Reminder Description"))
        // close keyboard after type title text and description
        Espresso.closeSoftKeyboard()
        // click on pick location
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation)).perform(ViewActions.click())
        // select map location
        Espresso.onView(ViewMatchers.withId(R.id.map)).perform(ViewActions.longClick())
        // click save location
        Espresso.onView(ViewMatchers.withId(R.id.save_location_btn)).perform(ViewActions.click())
        // click on save reminder => expected validation error in location
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())
        // close activity
        activityScenario.close()
    }


    // show toast message when saving new reminder success
    @Test
    fun saveReminderScreen_showToastMessage() {
        // start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        // navigate to save reminder page
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // type reminder data
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Title"))
        Espresso.closeSoftKeyboard()
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("Description"))
        Espresso.closeSoftKeyboard()
        // click on pick location
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.map)).perform(ViewActions.longClick())
        // click save location
        Espresso.onView(ViewMatchers.withId(R.id.save_location_btn)).perform(ViewActions.click())
        // click on save reminder => expected sucess
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())
        // display toast message
        Espresso.onView(ViewMatchers.withText(R.string.reminder_saved)).inRoot(
            RootMatchers.withDecorView(
                CoreMatchers.not(
                    CoreMatchers.`is`(
                        getActivity(activityScenario).window.decorView
                    )
                )
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // close activity
        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}
