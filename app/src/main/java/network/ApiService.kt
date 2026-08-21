package com.example.gibddochevidets.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ApiService {

    // ============================================================
    // REGISTRATION
    // ============================================================

    @POST("api/v1/devices/register")
    suspend fun registerDevice(
        @Header("X-Client-App")
        clientApp: String,

        @Body
        request: RegisterDeviceRequest
    ): RegisterResponse


    // ============================================================
    // SEND MESSAGE
    // ============================================================

    @POST("api/v1/messages")
    suspend fun sendMessage(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Body
        request: SendMessageRequest
    ): MessageResponse
// ============================================================
// DOWNLOAD MEDIA
// ============================================================

    @GET("api/v1/messages/{message_id}/media")
    suspend fun downloadMedia(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("message_id")
        messageId: String
    ): ResponseBody

    // ============================================================
    // MEDIA UPLOAD
    // ============================================================

    @Multipart
    @POST("api/v1/messages/media/upload")
    suspend fun uploadMedia(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Part
        file: MultipartBody.Part
    ): MessageResponse


    // ============================================================
    // GET CHAT MESSAGES
    // ============================================================

    @GET("api/v1/chats/{observer_device_id}/messages")
    suspend fun getMessages(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("observer_device_id")
        observerDeviceId: String,

        @Query("after_message_id")
        afterMessageId: String?,

        @Query("limit")
        limit: Int
    ): MessagesResponse


    // ============================================================
    // MARK DELIVERED
    // ============================================================

    @PATCH("api/v1/messages/{message_id}/delivered")
    suspend fun markDelivered(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("message_id")
        messageId: String
    ): MessageResponse


    // ============================================================
    // STATIC LOCATION
    // ============================================================

    @POST("api/v1/messages/static-location")
    suspend fun sendStaticLocation(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Body
        request: StaticLocationRequest
    ): MessageResponse


    // ============================================================
    // MEDIA
    // ============================================================

    @POST("api/v1/messages/media")
    suspend fun sendMedia(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Body
        request: MediaRequest
    ): MessageResponse


    // ============================================================
    // START LIVE LOCATION
    // ============================================================

    @POST("api/v1/messages/live-location/start")
    suspend fun startLiveLocation(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Body
        request: LiveLocationStartRequest
    ): MessageResponse


    // ============================================================
    // LIVE LOCATION POINT
    // ============================================================

    @POST("api/v1/messages/{message_id}/live-location/points")
    suspend fun sendLiveLocationPoint(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("message_id")
        messageId: String,

        @Body
        request: LiveLocationPointRequest
    ): LiveLocationPointResponse


    // ============================================================
    // STOP LIVE LOCATION
    // ============================================================

    @POST("api/v1/messages/{message_id}/live-location/stop")
    suspend fun stopLiveLocation(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("message_id")
        messageId: String
    ): MessageResponse


    // ============================================================
    // GET LIVE LOCATION POINTS
    // ============================================================

    @GET("api/v1/messages/{message_id}/live-location/points")
    suspend fun getLiveLocationPoints(
        @Header("Authorization")
        authorization: String,

        @Header("X-Client-App")
        clientApp: String,

        @Path("message_id")
        messageId: String,

        @Query("after_recorded_at")
        afterRecordedAt: String?,

        @Query("limit")
        limit: Int
    ): LiveLocationPointsResponse
}