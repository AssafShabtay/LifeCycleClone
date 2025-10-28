package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.model.PlaceEntity
import com.yourcompany.lifecycleclone.core.repository.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

/**
 * ViewModel for the places screen.
 *
 * - Exposes a list of all saved places as StateFlow.
 * - Allows adding and deleting places.
 */
class PlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlaceRepository

    private val _places = MutableStateFlow<List<PlaceEntity>>(emptyList())
    val places: StateFlow<List<PlaceEntity>> get() = _places

    init {
        val dao = AppDatabase.getInstance(application).placeDao()
        repository = PlaceRepository(dao)
        startObservingPlaces()
    }

    /**
     * Start collecting the Flow from the repository and mirror it into _places.
     * This runs in viewModelScope so it cancels automatically when the VM is cleared.
     */
    private fun startObservingPlaces() {
        viewModelScope.launch {
            repository.observeAllPlaces().collectLatest { list ->
                _places.value = list
            }
        }
    }

    /**
     * Insert a new place with some default radius/color.
     * radiusMeters and colorArgb defaults can later become user-editable.
     */
    fun addPlace(
        label: String,
        latitude: Double,
        longitude: Double,
        category: String
    ) {
        viewModelScope.launch {
            val place = PlaceEntity(
                label = label,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = 200f,     // default ~200m geofence
                category = category,
                colorArgb = 0xFF6A8CAF // make sure it's Int if your column is Int
            )
            repository.insertPlace(place)
        }
    }

    fun deletePlace(placeId: Long) {
        viewModelScope.launch {
            repository.deletePlace(placeId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // IMPORTANT: pull Application using ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                val app = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application
                PlacesViewModel(app)
            }
        }
    }
}
