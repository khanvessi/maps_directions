package com.example.maps.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maps.models.Direction
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var isMarkerClicked = MutableLiveData(false)
    var lineOption = MutableLiveData<PolylineOptions>()
    var markerTag = MutableLiveData<String>()
    val currentSelectedPagePos = MutableLiveData<Int>()
    var mDirectionList = ArrayList<Direction>()
    var listOfDirection = MutableLiveData<List<Direction>>()
    //val direction = Direction("", "",0f,0f)
}