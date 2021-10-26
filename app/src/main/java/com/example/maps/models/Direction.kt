package com.example.maps.models

data class Direction(
    var path: String = "",
    var origin: String = "",
    var address: String = "",
    var markerTag: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0
    ) {

}