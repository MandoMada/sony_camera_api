package com.example.sonybulb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SonyCameraClient(private val baseUrl: String) {
    suspend fun startRecMode(): Result<String> {
        return call("startRecMode", emptyList())
    }

    suspend fun setShootMode(mode: String): Result<String> {
        return call("setShootMode", listOf(mode))
    }

    suspend fun startBulbShooting(): Result<String> {
        return call("startBulbShooting", emptyList())
    }

    suspend fun stopBulbShooting(): Result<String> {
        return call("stopBulbShooting", emptyList())
    }

    private suspend fun call(method: String, params: List<Any>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject(
                    mapOf(
                        "method" to method,
                        "params" to params,
                        "id" to 1,
                        "version" to "1.0"
                    )
                ).toString()

                val url = URL("$baseUrl/sony/camera")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 10000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload)
                }

                val response = StringBuilder()
                val reader = if (connection.responseCode in 200..299) {
                    BufferedReader(connection.inputStream.reader())
                } else {
                    BufferedReader(connection.errorStream?.reader() ?: connection.inputStream.reader())
                }
                reader.useLines { lines ->
                    lines.forEach { response.append(it) }
                }

                if (connection.responseCode in 200..299) {
                    Result.success(response.toString())
                } else {
                    Result.failure(IllegalStateException("HTTP ${connection.responseCode}: $response"))
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }
}
