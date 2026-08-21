package com.example.gibddochevidets.data

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceFingerprint {

    fun get(
        context: Context
    ): String {

        val androidId =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown-device"

        return sha256(androidId)
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance("SHA-256")

        val bytes =
            digest.digest(
                value.toByteArray(Charsets.UTF_8)
            )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}