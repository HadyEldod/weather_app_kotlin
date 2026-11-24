package com.hady.weatherapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var tvTemperature: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var floatingActionButton: FloatingActionButton


    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var weatherApiService: WeatherApiService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent bars; icon color auto-adapts to the light/dark theme.
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val currentCity = intent.getStringExtra("cityName") ?: getLasSavedCityName()

        tvTemperature = findViewById(R.id.tvTemp)
        tvDescription = findViewById(R.id.tvStatus)
        tvCity = findViewById(R.id.tvCity)
        tvWind = findViewById(R.id.tvWind)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvVisibility = findViewById(R.id.tvVisibility)
        lottieAnimationView = findViewById(R.id.weatherAnimation)
        floatingActionButton = findViewById(R.id.fab)
        floatingActionButton.setOnClickListener(View.OnClickListener {
            openMainActivity()
        })
        val retrofit = Retrofit.Builder().baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(
                GsonConverterFactory.create()
            ).build()
        weatherApiService = retrofit.create(WeatherApiService::class.java)
if (currentCity != null) {
    searchWeather(currentCity)
    SaveCityName(currentCity)
}
//else {
//    searchWeather("London")   
//}

    }

    /**
     * App is edge-to-edge, so content draws behind the status and navigation bars.
     * Pad the root by the system-bar (and display-cutout) insets so nothing is hidden.
     * Padding the CoordinatorLayout also lifts the FAB above the navigation bar, and works
     * for both gesture navigation and 3-button navigation across Android 12–15+.
     */
    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.mainRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            windowInsets
        }
    }

    private fun searchWeather(cityName: String) {
        val apikey = "9b6c50bb105bedd76d5a866be0b3b0ef"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // units=metric -> temperature already in Celsius
                val response = weatherApiService.getWeather(cityName, apikey, "metric")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val weatherResponse = response.body()
                        weatherResponse?.let {
                            val temperature = it.main?.temp
                            val description = it.weather?.getOrNull(0)?.description
                            val city = it.name
                            updateWeatherUI(temperature, description, city)
                            updateAdditionalWeatherUI(it)
                        }
                    } else {
                        val message = when (response.code()) {
                            401 -> "Invalid API key. Activate or replace your OpenWeatherMap key."
                            404 -> "City \"$cityName\" not found"
                            429 -> "Too many requests. Try again shortly."
                            else -> "Error ${response.code()}. Please try again."
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateWeatherUI(temperature: Double?, description: String?, city: String?) {
        temperature?.let {
            // API queried with units=metric, so value is already in Celsius
            tvTemperature.text = "${it.toInt()}°C"
        }
        description?.let {
            tvDescription.text = it
            updateWeatherAnimation(it)
        }
        city?.let {
            tvCity.text = it
        }

    }

    private fun updateWeatherAnimation(weatherDescription: String) {
        val animationResId = when (weatherDescription.lowercase(Locale.getDefault())) {
            "clear sky" -> R.raw.weather_sunny
            "snow" -> R.raw.snow_icon
            "rain" -> R.raw.rainy_icon
            else -> R.raw.weather_storm
        }
        lottieAnimationView.setAnimation(animationResId)
        lottieAnimationView.playAnimation()

    }

    private fun updateAdditionalWeatherUI(weatherResponse: WeatherResponse) {
        weatherResponse.wind?.let {
            val windSpeed = it.speed ?: 0.0
            tvWind.text = "Wind Speed: ${windSpeed} m/s"

        }
        weatherResponse.main?.let {
            val humidity = it.humidity ?: 0.0
            tvHumidity.text = "humidity: ${humidity}%"
        }
        tvVisibility.text = "Visibility: ${weatherResponse.visibility ?: "N/A"} meters"
    }


    private fun openMainActivity() {
        val intent = Intent(this, Search_Activity::class.java)
        startActivity(intent)
        finish()
    }

    private fun SaveCityName(cityName: String) {
        val editor = sharedPreferences.edit()
        editor.putString("Last_City_Name", cityName)
        editor.apply()
    }

    private fun getLasSavedCityName(): String? {
        return sharedPreferences.getString("Last_City_Name", null)
    }

    interface WeatherApiService {
        @GET("weather")
        suspend fun getWeather(
            @Query("q") cityName: String,
            @Query("appid") apikey: String,
            @Query("units") units: String,
        ): Response<WeatherResponse>
    }

    data class WeatherResponse(
        val main: Main?,
        val weather: List<Weather>?,
        val name: String?,
        val rain: Rain?,
        val wind: Wind?,
        val visibility: Int?,
        @SerializedName("uvi")
        val uviIndex: Double?,

        )

    data class Main(
        val temp: Double?,
        val humidity: Int?
    )

    data class Weather(
        val description: String?,
    )

    data class Rain(
        @SerializedName("1h")
        val oneHour: Double?
    )


    data class Wind(
        val speed: Double?,
    )

}