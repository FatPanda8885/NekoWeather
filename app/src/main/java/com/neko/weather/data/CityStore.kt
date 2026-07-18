package com.neko.weather.data

import android.content.Context
import com.neko.weather.model.CityLocation
import org.json.JSONArray
import org.json.JSONObject

class CityStore(context: Context) {
    private val preferences = context.getSharedPreferences("neko_weather", Context.MODE_PRIVATE)

    fun load(): List<CityLocation> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    CityLocation(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        admin = item.optString("admin"),
                        country = item.optString("country"),
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        isCurrentLocation = item.optBoolean("isCurrentLocation"),
                        showOnHome = item.optBoolean("showOnHome", true),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun add(city: CityLocation): List<CityLocation> {
        val updated = (listOf(city) + load().filterNot {
            it.id == city.id || (it.latitude == city.latitude && it.longitude == city.longitude)
        }).take(12)
        save(updated)
        return updated
    }

    fun delete(id: String): List<CityLocation> {
        val updated = load().filterNot { it.id == id }
        save(updated)
        return updated
    }

    fun setShowOnHome(id: String, enabled: Boolean): List<CityLocation> {
        val updated = load().map { if (it.id == id) it.copy(showOnHome = enabled) else it }
        save(updated)
        return updated
    }

    private fun save(cities: List<CityLocation>) {
        val array = JSONArray(cities.map { city ->
            JSONObject()
                .put("id", city.id)
                .put("name", city.name)
                .put("admin", city.admin)
                .put("country", city.country)
                .put("latitude", city.latitude)
                .put("longitude", city.longitude)
                .put("isCurrentLocation", city.isCurrentLocation)
                .put("showOnHome", city.showOnHome)
        })
        preferences.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "savedCities"
    }
}
