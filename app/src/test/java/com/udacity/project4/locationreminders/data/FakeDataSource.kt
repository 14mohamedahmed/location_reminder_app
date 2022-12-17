package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    private var shouldReturnError = false

    // this function to force return error to test if function failed what happen
    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    // get all reminders if shouldReturnError not equal to true otherwise we need to return an fakse
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // if set to true we will return error message
        if (shouldReturnError) {
            return Result.Error(
                "Error getting reminders"
            )
        }
        // if reminders not empty return all and make result is success
        return com.udacity.project4.locationreminders.data.dto.Result.Success(reminders)
    }

    // save reminder in database
    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    // get reminder by given id if exist return it if shouldReturnError is false otherwise return error message
    // that we didn't find the reminder (reminder not found!)
    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        var reminder = reminders.find {
            it.id == id
        }
        if (shouldReturnError) {
            return Result.Error("Reminder not found!")
        }
        return Result.Success(reminder!!)
    }

    // delete all reminder from database
    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}