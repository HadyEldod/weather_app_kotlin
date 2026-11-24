package com.hady.weatherapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class Search_Activity : AppCompatActivity() {
    private lateinit var editText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent bars; icon color auto-adapts to the light/dark theme.
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        // Edge-to-edge: pad the root by system-bar/cutout insets so content isn't
        // drawn under the status or navigation bars (gesture and 3-button nav).
        val root = findViewById<View>(R.id.main)
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

        editText = findViewById(R.id.editText)
        val nextButton: Button = findViewById(R.id.nextButton)
        nextButton.setOnClickListener {
            val cityName = editText.text.toString().trim()
            if (cityName.isNotEmpty()) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("cityName", cityName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "please enter city name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}