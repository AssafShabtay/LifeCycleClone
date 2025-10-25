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

/**
 * ViewModel for the places screen.  It loads all places from the database and exposes them as
 * state.  It also provides operations to add and remove places.  Editing the name or radius
 * would require additional DAO methods and is left as future work.
 */
class PlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaceRepository
    private val _places = MutableStateFlow<List<PlaceEntity>>(emptyList())
    val places: StateFlow<List<PlaceEntity>> get() = _places

    init {
        val dao = AppDatabase.getInstance(application).placeDao()
        repository = PlaceRepository(dao)
        refreshPlaces()
    }

    private fun refreshPlaces() {
        viewModelScope.launch {
            repository.observeAllPlaces().collect { list ->
                _places.value = list
            }
        }
    }

    fun addPlace(label: String, latitude: Double, longitude: Double, category: String) {
        viewModelScope.launch {
            val place = PlaceEntity(
                label = label,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = 200f,
                category = category,
                colorArgb = 0xFF6A8CAF // default color; should be random or chosen by user
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
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

                PlacesViewModel(app)
            }
        }
    }
}