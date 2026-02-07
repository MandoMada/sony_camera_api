package com.example.sonybulb

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var shootingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraIpInput = findViewById<EditText>(R.id.camera_ip)
        val shotCountInput = findViewById<EditText>(R.id.shot_count)
        val exposureInput = findViewById<EditText>(R.id.exposure_seconds)
        val intervalInput = findViewById<EditText>(R.id.interval_seconds)
        val statusView = findViewById<TextView>(R.id.status_text)
        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)

        startButton.setOnClickListener {
            shootingJob?.cancel()

            val cameraIp = cameraIpInput.text.toString().ifBlank { DEFAULT_CAMERA_IP }
            val shotCount = shotCountInput.text.toString().toIntOrNull() ?: 1
            val exposureSeconds = exposureInput.text.toString().toLongOrNull() ?: 1L
            val intervalSeconds = intervalInput.text.toString().toLongOrNull() ?: 1L

            val client = SonyCameraClient("http://$cameraIp:10000")
            shootingJob = lifecycleScope.launch {
                statusView.text = "Connecting to camera..."
                client.startRecMode()
                client.setShootMode("bulb")

                var currentShot = 1
                while (isActive && currentShot <= shotCount) {
                    statusView.text = "Shot $currentShot of $shotCount: opening shutter"
                    val startResult = client.startBulbShooting()
                    if (startResult.isFailure) {
                        statusView.text = "Failed to start bulb: ${startResult.exceptionOrNull()?.message}"
                        break
                    }

                    delay(exposureSeconds * 1000)

                    statusView.text = "Shot $currentShot of $shotCount: closing shutter"
                    val stopResult = client.stopBulbShooting()
                    if (stopResult.isFailure) {
                        statusView.text = "Failed to stop bulb: ${stopResult.exceptionOrNull()?.message}"
                        break
                    }

                    if (currentShot < shotCount) {
                        statusView.text = "Waiting ${intervalSeconds}s before next shot"
                        delay(intervalSeconds * 1000)
                    }
                    currentShot += 1
                }

                if (isActive) {
                    statusView.text = "Done"
                }
            }
        }

        stopButton.setOnClickListener {
            shootingJob?.cancel()
            statusView.text = "Stopped"
        }
    }

    companion object {
        private const val DEFAULT_CAMERA_IP = "192.168.122.1"
    }
}
