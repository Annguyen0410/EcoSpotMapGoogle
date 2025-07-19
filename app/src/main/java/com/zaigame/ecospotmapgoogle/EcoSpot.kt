package com.zaigame.ecospotmapgoogle

import com.google.android.gms.maps.model.LatLng

// An enum to define the different types of spots. This is much safer than using strings.
enum class SpotType {
    RECYCLING_CENTER,
    THRIFT_STORE,
    FARMERS_MARKET
}

// A data class to hold all the information about one single eco-friendly spot.
data class EcoSpot(
    val name: String,
    val latLng: LatLng,
    val type: SpotType,
    val description: String
)