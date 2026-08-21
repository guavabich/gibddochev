package com.example.gibddochevidets.network

import retrofit2.HttpException
import android.util.Log
import okhttp3.ResponseBody
import android.content.Context
import android.net.Uri
import com.example.gibddochevidets.data.DeviceFingerprint
import com.example.gibddochevidets.data.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class ApiRepository(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val session =
        SessionManager(appContext)

    private val api: ApiService =
        Retrofit.Builder()
            .baseUrl(
                "https://xn--e1afhclgq.xn--p1ai:4401/"
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(
                ApiService::class.java
            )


    // ============================================================
    // CONSTANTS
    // ============================================================

    private val clientApp =
        "eyewitness"


    // ============================================================
    // AUTH / REGISTRATION
    // ============================================================

    suspend fun registerDevice(): RegisterResponse {

        val fingerprint =
            DeviceFingerprint.get(
                appContext
            )

        Log.e(
            "REGISTER_DEBUG",
            "========== REGISTER =========="
        )

        Log.e(
            "REGISTER_DEBUG",
            "fingerprint = $fingerprint"
        )

        Log.e(
            "REGISTER_DEBUG",
            "fingerprint length = ${fingerprint.length}"
        )

        Log.e(
            "REGISTER_DEBUG",
            "clientApp = $clientApp"
        )

        try {

            val result =
                api.registerDevice(

                    clientApp =
                        clientApp,

                    request =
                        RegisterDeviceRequest(

                            fingerprint_hash =
                                fingerprint,

                            push_token =
                                null
                        )
                )

            Log.e(
                "REGISTER_DEBUG",
                "REGISTER SUCCESS"
            )

            session.deviceId =
                result.device_id

            session.accessToken =
                result.access_token

            session.fingerprintHash =
                fingerprint

            return result

        } catch (e: HttpException) {

            Log.e(
                "REGISTER_DEBUG",
                "HTTP CODE = ${e.code()}"
            )

            Log.e(
                "REGISTER_DEBUG",
                "HTTP MESSAGE = ${e.message()}"
            )

            val errorBody =
                e.response()
                    ?.errorBody()
                    ?.string()

            Log.e(
                "REGISTER_DEBUG",
                "SERVER BODY = $errorBody"
            )

            throw e

        } catch (e: Exception) {

            Log.e(
                "REGISTER_DEBUG",
                "OTHER ERROR",
                e
            )

            throw e
        }
    }


    // ============================================================
    // SEND TEXT MESSAGE
    // ============================================================

    suspend fun sendMessage(
        text: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        val cleanText =
            text.trim()

        require(
            cleanText.isNotEmpty()
        ) {
            "Сообщение не может быть пустым"
        }

        return api.sendMessage(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            request =
                SendMessageRequest(

                    message_type =
                        "TEXT",

                    observer_device_id =
                        null,

                    text =
                        cleanText
                )
        )
    }


    // ============================================================
    // GET MESSAGES
    // ============================================================

    suspend fun getMessages(
        afterMessageId: String? = null
    ): List<MessageResponse> {

        val token =
            getRequiredToken()

        val deviceId =
            getRequiredDeviceId()

        return api.getMessages(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            observerDeviceId =
                deviceId,

            afterMessageId =
                afterMessageId,

            limit =
                100
        ).messages
    }

// ============================================================
// DOWNLOAD MEDIA FILE
// ============================================================

    suspend fun downloadMedia(
        messageId: String
    ): ByteArray {

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        val token =
            getRequiredToken()

        requireRegistered()

        val response =
            api.downloadMedia(

                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                messageId =
                    messageId
            )

        return response.bytes()
    }
    // ============================================================
    // MARK MESSAGE DELIVERED
    // ============================================================

    suspend fun markDelivered(
        messageId: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        return api.markDelivered(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId
        )
    }


    // ============================================================
    // SEND STATIC LOCATION
    // ============================================================

    suspend fun sendStaticLocation(
        latitude: Double,
        longitude: Double
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            latitude in -90.0..90.0
        ) {
            "Некорректная широта"
        }

        require(
            longitude in -180.0..180.0
        ) {
            "Некорректная долгота"
        }

        return api.sendStaticLocation(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            request =
                StaticLocationRequest(

                    latitude =
                        latitude,

                    longitude =
                        longitude
                )
        )
    }


    // ============================================================
    // SEND MEDIA
    // ============================================================

    suspend fun sendMedia(
        storageKey: String,
        mimeType: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            storageKey.isNotBlank()
        ) {
            "storageKey пустой"
        }

        require(
            mimeType.isNotBlank()
        ) {
            "mimeType пустой"
        }

        return api.sendMedia(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            request =
                MediaRequest(

                    storage_key =
                        storageKey,

                    mime_type =
                        mimeType
                )
        )

    }
    // ============================================================
    // UPLOAD MEDIA FILE FROM URI
    // ============================================================

    suspend fun uploadMedia(
        uri: Uri
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        val resolver =
            appContext.contentResolver

        val mimeType =
            resolver.getType(uri)
                ?: "application/octet-stream"

        val bytes =
            resolver.openInputStream(uri)?.use {
                it.readBytes()
            }
                ?: throw IllegalStateException(
                    "Не удалось открыть выбранный файл"
                )

        require(bytes.isNotEmpty()) {
            "Выбранный файл пустой"
        }

        val fileName =
            getFileName(uri)

        val requestBody =
            bytes.toRequestBody(
                mimeType.toMediaTypeOrNull()
            )

        val filePart =
            MultipartBody.Part.createFormData(
                "file",
                fileName,
                requestBody
            )

        return api.uploadMedia(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            file =
                filePart
        )
    }


    // ============================================================
    // START LIVE LOCATION
    // ============================================================

    suspend fun startLiveLocation():
            MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        return api.startLiveLocation(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            request =
                LiveLocationStartRequest(
                    observer_device_id =
                        null
                )
        )
    }


    // ============================================================
    // SEND LIVE LOCATION POINT
    // ============================================================

    suspend fun sendLiveLocationPoint(
        messageId: String,
        latitude: Double,
        longitude: Double
    ): LiveLocationPointResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        require(
            latitude in -90.0..90.0
        ) {
            "Некорректная широта"
        }

        require(
            longitude in -180.0..180.0
        ) {
            "Некорректная долгота"
        }

        return api.sendLiveLocationPoint(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId,

            request =
                LiveLocationPointRequest(

                    latitude =
                        latitude,

                    longitude =
                        longitude
                )
        )
    }


    // ============================================================
    // STOP LIVE LOCATION
    // ============================================================

    suspend fun stopLiveLocation(
        messageId: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        return api.stopLiveLocation(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId
        )
    }


    // ============================================================
    // GET LIVE LOCATION POINTS
    // ============================================================

    suspend fun getLiveLocationPoints(
        messageId: String,
        afterRecordedAt: String? = null,
        limit: Int = 100
    ): List<LiveLocationPointResponse> {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        require(
            limit in 1..1000
        ) {
            "limit должен быть от 1 до 1000"
        }

        return api.getLiveLocationPoints(

            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId,

            afterRecordedAt =
                afterRecordedAt,

            limit =
                limit

        ).points
    }

    // ============================================================
    // GET FILE NAME FROM URI
    // ============================================================

    private fun getFileName(
        uri: Uri
    ): String {

        var fileName =
            "media_${System.currentTimeMillis()}"

        val cursor =
            appContext.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )

        cursor?.use {

            if (it.moveToFirst()) {

                val index =
                    it.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {

                    val name =
                        it.getString(index)

                    if (
                        !name.isNullOrBlank()
                    ) {
                        fileName = name
                    }
                }
            }
        }

        return fileName
    }
    // ============================================================
    // SESSION
    // ============================================================

    fun getDeviceId(): String? {

        return session.deviceId
    }


    fun getAccessToken(): String? {

        return session.accessToken
    }


    fun isRegistered(): Boolean {

        return session.isRegistered()
    }


    // ============================================================
    // REQUIRED TOKEN
    // ============================================================

    private fun getRequiredToken():
            String {

        return session.accessToken
            ?: throw IllegalStateException(
                "Access token is missing"
            )
    }


    // ============================================================
    // REQUIRED DEVICE
    // ============================================================

    private fun getRequiredDeviceId():
            String {

        return session.deviceId
            ?: throw IllegalStateException(
                "Device is not registered"
            )
    }


    // ============================================================
    // REQUIRED REGISTERED
    // ============================================================

    private fun requireRegistered() {

        if (
            session.deviceId == null
        ) {

            throw IllegalStateException(
                "Device is not registered"
            )
        }

        if (
            session.accessToken == null
        ) {

            throw IllegalStateException(
                "Access token is missing"
            )
        }
    }
}