package com.example.reactivedemo

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.Uninitialized

data class MavericksModel(val breedsState: Async<List<Breed>> = Uninitialized): MavericksState

class MainViewModel(initialState: MavericksModel): MavericksViewModel<MavericksModel>(initialState) {
    fun queryUpdated(query: String) = suspend { searchBreeds(query) }.execute {
        MavericksModel(it)
    }
}