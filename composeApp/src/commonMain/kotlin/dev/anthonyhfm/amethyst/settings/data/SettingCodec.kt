package dev.anthonyhfm.amethyst.settings.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

interface SettingCodec<T> {
    fun encode(value: T): String
    fun decode(raw: String): T

    companion object {
        val Boolean: SettingCodec<kotlin.Boolean> = object : SettingCodec<kotlin.Boolean> {
            override fun encode(value: kotlin.Boolean) = value.toString()
            override fun decode(raw: String) = raw.toBooleanStrict()
        }

        val Int: SettingCodec<kotlin.Int> = object : SettingCodec<kotlin.Int> {
            override fun encode(value: kotlin.Int) = value.toString()
            override fun decode(raw: String) = raw.toInt()
        }

        val Float: SettingCodec<kotlin.Float> = object : SettingCodec<kotlin.Float> {
            override fun encode(value: kotlin.Float) = value.toString()
            override fun decode(raw: String) = raw.toFloat()
        }

        val String: SettingCodec<kotlin.String> = object : SettingCodec<kotlin.String> {
            override fun encode(value: kotlin.String) = value
            override fun decode(raw: kotlin.String) = raw
        }

        fun <T : Any> json(serializer: KSerializer<T>): SettingCodec<T> = object : SettingCodec<T> {
            override fun encode(value: T): kotlin.String = Json.encodeToString(serializer, value)
            override fun decode(raw: kotlin.String): T = Json.decodeFromString(serializer, raw)
        }

        inline fun <reified T : Any> json(): SettingCodec<T> = json(serializer())
    }
}
