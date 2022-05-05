package com.example.reactivedemo

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.Uninitialized
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mavericks is a framework like Elm, but with slightly more freedom to handle state how you want.
 * */
data class MavericksModel(val breedsState: Async<List<Breed>> = Uninitialized) : MavericksState

class MainViewModel(initialState: MavericksModel) :
    MavericksViewModel<MavericksModel>(initialState) {

    private var query: String? = null

    /**
     *  This particular case is worse than normal imperative code.
     *  - Good guardrails
     */
    fun queryUpdated(query: String) {
        this.query = query
        viewModelScope.launch {
            delay(500)
            if (this@MainViewModel.query == query) {
                suspend {
                    searchBreeds(query)
                }.execute { copy(it) }
            }
        }
    }
}