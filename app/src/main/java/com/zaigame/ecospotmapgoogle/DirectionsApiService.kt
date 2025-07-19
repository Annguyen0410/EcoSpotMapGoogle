package com.zaigame.ecospotmapgoogle

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes to hold only the parts of the JSON response we need
data class DirectionsResponse(val routes: List<Route>)
data class Route(val overview_polyline: OverviewPolyline)
data class OverviewPolyline(val points: String)

// Interface for Retrofit to create the API call
interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>
}