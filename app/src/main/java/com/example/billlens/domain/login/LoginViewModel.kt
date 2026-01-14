package com.example.billlens.domain.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LoginState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onSignInResult(isSuccess: Boolean, errorMessage: String?) {
        _state.update {
            it.copy(
                isSignInSuccessful = isSuccess,
                signInError = if (isSuccess) null else errorMessage
            )
        }
    }

    fun resetState() {
        _state.update { LoginState() }
    }
}