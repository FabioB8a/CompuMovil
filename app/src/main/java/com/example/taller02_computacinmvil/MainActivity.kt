package com.example.taller02_computacinmvil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taller02_computacinmvil.databinding.ActivityMainBinding

/**
 *  Coded by: Fabio Luis Buitrago Ochoa
 */

class MainActivity : AppCompatActivity() {

    /**
     * 1. Defina una aplicación usando el layout de su preferencia y dentro del mismo ubique dos botones
     * con imágenes. El resultado debe verse similar a la figura 1.
     */

    // View binding instance for the main activity's layout
    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           then this Bundle contains the data it most recently supplied in
     *                           onSaveInstanceState(Bundle). Note: Otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
    }

    /**
     * Set up the behavior for the "Camera" and "Map" buttons.
     */
    private fun init(){
        // Set up a click listener for the "Camera" button
        binding.btnCamera.setOnClickListener(){
            // Create an Intent to start the "CameraActivity" when the button is clicked
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // Set up a click listener for the "Map" button
        binding.btnMap.setOnClickListener(){
            // Create an Intent to start the "MapsActivity" when the button is clicked
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}