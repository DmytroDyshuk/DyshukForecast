package com.example.dyshukforecast

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.dyshukforecast.model.Weather
import com.example.dyshukforecast.presenter.ForecastAdapter
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val AUTOCOMPLETE_REQUEST_CODE = 54

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidNetworking.initialize(applicationContext)
        makeRequest(null)

        ivSearch.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }
    }



    private fun makeRequest(latLng: LatLng?) {
        AndroidNetworking.get("https://api.openweathermap.org/data/2.5/onecall")
            .addQueryParameter("lat", "${latLng?.latitude ?: 50.4501}" )
            .addQueryParameter("lon", "${latLng?.longitude ?: 30.5234}")
            .addQueryParameter("appid", "eb7d05cd954e0b91e47b916c41b3cd31")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val main = response.getJSONObject("current")
                    val temperature = main.getInt("temp")

                    val forecastList = ArrayList<Weather>()
                    val weatherArray = main.getJSONArray("weather")
                    for (i in 0 until weatherArray.length()) {
                        val weatherResult = weatherArray.getJSONObject(i)
                        val description = weatherResult.getString("description")

                        tvDescription.text = description
                    }

                    if(temperature.toCelsius() < 0) {
                        ivWeatherIcon.setImageResource(R.drawable.vector_snow_icon)
                    }

                    tvTemperature.text = temperature.toPresentableCelsius()

                    val hourlyArray = response.getJSONArray("hourly")
                    for (i in 0 until hourlyArray.length()) {
                        val hourlyResult = hourlyArray.getJSONObject(i)
                        val hourlyTemp = hourlyResult.getInt("temp")
                        val dt = hourlyResult.getLong("dt")



                        val weather = Weather()
                        weather.temperature = hourlyTemp
                        weather.time = dt
                        forecastList.add(weather)
                    }

                    val adapter = ForecastAdapter(this@MainActivity, forecastList)
                    vpForecast.adapter = adapter
                }

                override fun onError(error: ANError?) {
                    Log.e("Network", "onError:$error")
                }
            })
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.i("PojiloyResult", "Place: ${place.name}, ${place.id}, ${place.latLng?.latitude}, ${place.latLng?.longitude}")
                        tvCity.text = place.name
                        makeRequest(place.latLng)
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    Toast.makeText(this, "Что-то пошло не так", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}