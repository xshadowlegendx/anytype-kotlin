package com.agileburo.anytype.presentation.auth.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agileburo.anytype.core_utils.common.EventWrapper
import com.agileburo.anytype.domain.auth.interactor.StartAccount
import com.agileburo.anytype.domain.device.PathProvider
import com.agileburo.anytype.presentation.navigation.AppNavigation
import com.agileburo.anytype.presentation.navigation.SupportNavigation
import timber.log.Timber

class SetupSelectedAccountViewModel(
    private val startAccount: StartAccount,
    private val pathProvider: PathProvider
) : ViewModel(), SupportNavigation<EventWrapper<AppNavigation.Command>> {

    val error = MutableLiveData<String>()

    override val navigation: MutableLiveData<EventWrapper<AppNavigation.Command>> =
        MutableLiveData()

    fun selectAccount(id: String) {
        startAccount.invoke(
            scope = viewModelScope,
            params = StartAccount.Params(
                id = id,
                path = pathProvider.providePath()
            )
        ) { result ->
            result.either(
                fnL = {
                    error.postValue(ERROR_MESSAGE)
                    Timber.e(it, "Error while selecting account with id: $id")
                },
                fnR = { navigateToHomeDashboard() }
            )
        }
    }

    private fun navigateToHomeDashboard() {
        navigation.postValue(EventWrapper(AppNavigation.Command.StartDesktopFromLogin))
    }

    companion object {
        const val ERROR_MESSAGE = "An error occured while starting account..."
    }
}