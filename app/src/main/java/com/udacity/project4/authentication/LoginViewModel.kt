package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map


class LoginViewModel : ViewModel() {
    enum class State {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }
    // check current user state is authenticated or not
    val state = FirebaseUserLiveData().map {
        if (it != null) {
            State.AUTHENTICATED
        } else {
            State.UNAUTHENTICATED
        }
    }
}