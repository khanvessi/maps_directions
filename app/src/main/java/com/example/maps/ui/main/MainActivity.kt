package com.example.maps.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.maps.R
import com.example.maps.databinding.ActivityMainBinding
import com.example.maps.models.Direction
import com.example.maps.ui.DirectionFragment
import com.example.maps.utils.GoogleMapDTO
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var polygon: Polygon? = null
    var gMap: GoogleMap? = null
    lateinit var binding: ActivityMainBinding
    private val MY_PERMISSIONS_REQUEST_LOCATION = 101
    private val STORAGE_PERMISSION_CODE = 102
    private val markerOptions = MarkerOptions()
    var server: LatLng? = null
    var destination: LatLng? = null
    var direction: Direction? = null
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val directionFragment = DirectionFragment()

        binding.apply {
            cvZoom.setOnClickListener(View.OnClickListener {
                //navigateToMyLocation()
            })

            cvMinus.setOnClickListener(View.OnClickListener {
                //TODO: DECREASE ZOOM LEVEL
            })

            cvPlus.setOnClickListener(View.OnClickListener {
                //TODO: INCREASE ZOOM LEVEL
            })

            btnPolygamy.setOnClickListener(View.OnClickListener {
//                polygon?.remove()
//                val polygonOptions = PolygonOptions()
//                polygonOptions.addAll(mutableListLatLng).clickable(true)
//                polygon = gMap?.addPolygon(polygonOptions)
            })
        }

        directionFragment.show(supportFragmentManager, directionFragment.tag)

        mainViewModel.currentSelectedPagePos.observe(this){
            Log.e(TAG, "onCreate: Current Page Position, $it", )
            gMap?.clear()
            val tempMarker = LatLng(33.62407496836641, 73.07110647253837)
            gMap?.addMarker(MarkerOptions().position(tempMarker).title("Server"))
            val currentPage = mainViewModel.listOfDirection.value
            val currPage = currentPage?.get(it)
            val curPageLatLong = currPage?.let { it1 -> LatLng(it1.lat,it1.lng) }
//            if (currPage != null) {
//                Log.e(TAG, "onCreate: CURRENT PAGE: " +currPage.lat+ currPage.lng, )
//                Log.e(TAG, "onCreate: CURRENT PAGE: $currPage", )
//            }
            gMap?.addMarker(MarkerOptions().position(curPageLatLong))

            val URL = curPageLatLong?.let { currPageLatLong -> getDirectionURL(tempMarker, currPageLatLong) }
            if (URL != null) {
                GetDirection(URL).execute()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        gMap = googleMap
        server = LatLng(33.62407496836641, 73.07110647253837)
        googleMap?.addMarker(MarkerOptions().position(server).title("Server"))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(server,15f))

        gMap?.setOnMarkerClickListener {marker ->
            val directionFragment = DirectionFragment()
            marker.tag = direction!!.address
            mainViewModel.markerTag.value = marker.tag.toString()
            directionFragment.show(supportFragmentManager, directionFragment.tag)
            Log.e(TAG, "onMarkerClick: ", )
            true
        }
        //GetGoogleMapImage().execute()

        gMap?.setOnMapClickListener { cords ->
            direction = Direction()
            destination = LatLng(cords.latitude,cords.longitude)
            googleMap?.clear()
            val tempMarker = LatLng(33.62407496836641, 73.07110647253837)
            googleMap?.addMarker(MarkerOptions().position(tempMarker).title("Server"))

            val dest = LatLng(cords.latitude, cords.longitude)

            var marker = googleMap?.addMarker(MarkerOptions().position(dest))
            marker?.tag = direction!!.address
            //mainViewModel.direction.address = getAddress(cords.latitude,cords.longitude)


            //ADDRESSES
            direction!!.origin = getAddress(tempMarker.latitude,tempMarker.longitude)
            Log.e(TAG, "onMapReady: "+ direction!!.origin, )
            direction!!.address = getAddress(cords.latitude,cords.longitude)


            //LATLONG
            direction!!.lat = cords.latitude
            direction!!.lng = cords.longitude

            //PLACE POLYLINE BETWEEN TWO MARKERS
            Log.d("GoogleMap", "before URL")
            val URL = getDirectionURL(tempMarker,dest)
            Log.d("GoogleMap", "URL : $URL")
            GetDirection(URL).execute()
            GetGoogleMapImage().execute()

            Handler().postDelayed({
                addDirection(direction!!)
            },2000)
        }
    }

    //AIzaSyCwxw7gJERjUGawcISxVU4mRlDoJsysLT8

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()
            Log.d("GoogleMap" , " data : $data")
            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)

                val path =  ArrayList<LatLng>()

                for (i in 0 until respObj.routes[0].legs[0].steps.size){
//                    val startLatLng = LatLng(respObj.routes[0].legs[0].steps[i].start_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].start_location.lng.toDouble())
//                    path.add(startLatLng)
//                    val endLatLng = LatLng(respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble())
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            Log.e(TAG, "onPostExecute:")
            gMap?.addPolyline(lineoption)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetGoogleMapImage : AsyncTask<Void, Void, Bitmap?>(){
        override fun doInBackground(vararg params: Void?): Bitmap? {
            val url = URL("https://maps.googleapis.com/maps/api/staticmap" +
                    "?center=" + server?.latitude +","+server?.longitude +
                    "&zoom=15&" +
                    "path=color:blue%7Cweight:10%7C"
                    + server?.latitude +","+server?.longitude + "%7C"
                    + destination?.latitude +","+destination?.longitude +
                    "&size=400x400&" +
                    "maptype=roadmap&" +
                    "markers=size:mid%7Ccolor:0xFFFF00%7Clabel:C%7C" + destination?.latitude + ","+destination?.longitude +
                    "&key=AIzaSyCwxw7gJERjUGawcISxVU4mRlDoJsysLT8"
            )
            Log.e(TAG, "doInBackground: $url", )

            var image: Bitmap? = null
            try {
                val stream =  url.openConnection().content
                image = BitmapFactory.decodeStream(stream as InputStream?)
                Log.e(TAG, "doInBackground: ${image.toString()}", )
            } catch (e: MalformedURLException){
                e.printStackTrace()
            } catch (e: IOException){
                e.printStackTrace()
            }
            return image
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            if (result != null) {
                checkPermissions(result)
            }
            Log.e(TAG, "onPostExecute: "+result.toString().isNullOrBlank(), )
            val directionFragment = DirectionFragment()
            directionFragment.show(supportFragmentManager, directionFragment.tag)
        }
    }

    //HELPING METHODS
    private fun getAddress(lat: Double, long:Double): String {
        try {
            val geo =
                Geocoder(this@MainActivity.applicationContext, Locale.getDefault())
            val addresses = geo.getFromLocation(lat, long, 1)
            if (addresses.isEmpty()) {
                //yourtextfieldname.setText("Waiting for Location")
            } else {
                if (addresses.size > 0) {
                    //yourtextfieldname.setText(addresses[0].featureName + ", " + addresses[0].locality + ", " + addresses[0].adminArea + ", " + addresses[0].countryName)
                    //Toast.makeText(getApplicationContext(), "Address:- " + addresses.get(0).getFeatureName() + addresses.get(0).getAdminArea() + addresses.get(0).getLocality(), Toast.LENGTH_LONG).show();
                    return addresses[0].featureName + ", " + addresses[0].locality + ", " + addresses[0].adminArea + ", " + addresses[0].countryName
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace() // getFromLocation() may sometimes fail
        }
        return ""
    }

    private fun getDirectionURL(origin:LatLng,dest:LatLng) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=driving&key=AIzaSyCwxw7gJERjUGawcISxVU4mRlDoJsysLT8"
    }

    private fun addDirection(direction: Direction) {
        mainViewModel.mDirectionList.add(direction)
        
        Log.e(TAG, "addIssuePost: ${mainViewModel.mDirectionList}", )
        mainViewModel.listOfDirection.value = mainViewModel.mDirectionList
        Log.e(TAG, "addIssuePost: "+mainViewModel.listOfDirection.value, )
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (applicationContext?.contentResolver != null) {
            val cursor: Cursor? = applicationContext?.contentResolver
                ?.query(uri, null, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Image", null)
        Log.e(TAG, "getImageUri: $path", )
        return Uri.parse(path)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(result: Bitmap) {
        Log.e(TAG, "checkPermissions: Called", )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                STORAGE_PERMISSION_CODE
            )
        } else {
            val uri = getImageUri(applicationContext, result)
            val realPath = getRealPathFromURI(uri)
            direction?.path  = realPath
            //Log.e(TAG, "afterPermission: "+mainViewModel.direction.path, )

        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        Log.e(TAG, "decodePolyline: " + poly[0])
        return poly
    }

    private fun getAddress(context: Context?, lat: Double, lng: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]
            var add: String = obj.getAddressLine(0)
            add = """
                 $add
                 ${obj.countryName}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.countryCode}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.adminArea}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.postalCode}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.subAdminArea}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.locality}
                 """.trimIndent()
            add = """
                 $add
                 ${obj.subThoroughfare}
                 """.trimIndent()
            add
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun navigateToMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        }else{
            gMap?.isMyLocationEnabled = true
            gMap?.setOnMyLocationChangeListener {
                val currentLocation = LatLng(it.latitude, it.longitude)
                Log.e(TAG, "navigateToMyLocation: " + it.latitude + "  " + it.longitude)
                markerOptions.position(currentLocation)
                gMap?.addMarker(
                    MarkerOptions().position(LatLng(it.latitude, it.longitude))
                        .title("It's My Location!")
                )
                gMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   navigateToMyLocation()
                }
            }
        }
    }
}