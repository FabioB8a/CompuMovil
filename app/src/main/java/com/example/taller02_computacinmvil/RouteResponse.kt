package com.example.taller02_computacinmvil

import com.google.gson.annotations.SerializedName

/**
 * Represents the response data for a route obtained from the API.
 *
 * @param features List of route features.
 */
data class RouteResponse(@SerializedName("features") val features: List<Feature>)

/**
 * Represents a feature of the route geometry.
 *
 * @param geometry The geometry of the feature.
 */
data class Feature(@SerializedName("geometry") val geometry: Geometry)

/**
 * Represents the geometry data for a route.
 *
 * @param coordinates List of coordinate points.
 */
data class Geometry(@SerializedName("coordinates") val coordinates: List<List<Double>>)