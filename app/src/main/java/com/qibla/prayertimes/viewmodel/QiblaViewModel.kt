package com.qibla.prayertimes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qibla.prayertimes.alarm.AlarmScheduler
import com.qibla.prayertimes.data.CityStore
import com.qibla.prayertimes.data.LocationHelper
import com.qibla.prayertimes.data.PrayerTimesRepository
import com.qibla.prayertimes.data.PrayerTimesState
import com.qibla.prayertimes.data.QiblaMath
import com.qibla.prayertimes.data.WidgetDataStore
import com.qibla.prayertimes.model.City
import com.qibla.prayertimes.model.defaultCities
import com.qibla.prayertimes.widget.QiblaWidgetUpdater
import com.qibla.prayertimes.work.PrayerTimesWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QiblaViewModel(application: Application) : AndroidViewModel(application) {

    private val cityStore = CityStore(application)
    private val locationHelper = LocationHelper(application)
    private val prayerRepo = PrayerTimesRepository()
    private val widgetDataStore = WidgetDataStore(application)

    private val _selectedCity = MutableStateFlow(cityStore.loadSelectedCity() ?: defaultCities(application).first())
    val selectedCity: StateFlow<City> = _selectedCity.asStateFlow()

    private val _customCities = MutableStateFlow(cityStore.loadCustomCities())
    val customCities: StateFlow<List<City>> = _customCities.asStateFlow()

    private val _prayerState = MutableStateFlow<PrayerTimesState>(PrayerTimesState.Loading)
    val prayerState: StateFlow<PrayerTimesState> = _prayerState.asStateFlow()

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    val bearing: Double get() = QiblaMath.bearing(_selectedCity.value.lat, _selectedCity.value.lon)
    val distanceKm: Int get() = QiblaMath.distanceKm(_selectedCity.value.lat, _selectedCity.value.lon)

    init {
        refreshPrayerTimes()
        PrayerTimesWorker.schedulePeriodic(application)
        com.qibla.prayertimes.work.WidgetRefreshWorker.schedulePeriodic(application)
    }

    fun selectCity(city: City) {
        _selectedCity.value = city
        cityStore.saveSelectedCity(city)
        refreshPrayerTimes()
    }

    fun refreshPrayerTimes() {
        val city = _selectedCity.value
        _prayerState.value = PrayerTimesState.Loading
        viewModelScope.launch {
            val result = prayerRepo.fetchToday(city.lat, city.lon)
            _prayerState.value = result
            if (result is PrayerTimesState.Success) {
                val app = getApplication<Application>()
                widgetDataStore.save(city.name, result.result.timings, result.result.hijri, result.result.isOffline)
                AlarmScheduler.scheduleToday(app, result.result.timings)
                QiblaWidgetUpdater.requestUpdate(app)
            }
        }
    }

    fun addCustomCity(city: City) {
        val updated = _customCities.value + city
        _customCities.value = updated
        cityStore.saveCustomCities(updated)
        selectCity(city)
    }

    fun removeCustomCity(city: City) {
        val updated = _customCities.value.filterNot { it.name == city.name && it.lat == city.lat && it.lon == city.lon }
        _customCities.value = updated
        cityStore.saveCustomCities(updated)
    }

    fun locateMe() {
        viewModelScope.launch {
            _locating.value = true
            val city = locationHelper.getCurrentCity()
            if (city != null) selectCity(city)
            _locating.value = false
        }
    }
}
