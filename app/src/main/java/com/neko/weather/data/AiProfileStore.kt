package com.neko.weather.data

import android.content.Context
import com.neko.weather.model.AiModelProfile
import com.neko.weather.model.AiProfiles
import com.neko.weather.model.AiProtocol
import com.neko.weather.model.AiSettings
import com.neko.weather.model.DEFAULT_AI_CONTEXT
import org.json.JSONArray
import org.json.JSONObject

class AiProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("neko_weather", Context.MODE_PRIVATE)

    fun load(legacy: AiSettings): AiProfiles {
        val raw = preferences.getString(KEY, null) ?: return migrate(legacy).also(::save)
        return runCatching { parse(JSONObject(raw)) }.getOrElse { migrate(legacy).also(::save) }
    }

    fun save(value: AiProfiles) {
        val root = JSONObject()
            .put("selectedId", value.selectedId)
            .put("models", JSONArray(value.models.map(::modelJson)))
        preferences.edit().putString(KEY, root.toString()).apply()
    }

    private fun migrate(legacy: AiSettings): AiProfiles {
        val protocol = AiProtocol.ANTHROPIC_MESSAGES
        val profile = AiModelProfile(
            id = MIGRATED_ID,
            name = legacy.model.ifBlank { "我的模型" },
            protocol = protocol,
            endpoint = legacy.endpoint.ifBlank { protocol.defaultEndpoint },
            apiKey = legacy.apiKey,
            model = legacy.model,
            maxTokens = legacy.maxTokens.coerceIn(64, 8192),
            maxOutputCharacters = legacy.maxOutputCharacters.coerceIn(100, 10000),
            context = legacy.context.ifBlank { DEFAULT_AI_CONTEXT },
            rawRequestJson = legacy.rawRequestJson.ifBlank { protocol.defaultRequestJson },
        )
        return AiProfiles(models = listOf(profile), selectedId = profile.id)
    }

    private fun parse(root: JSONObject): AiProfiles {
        val models = root.optJSONArray("models").objects().mapNotNull { item ->
            val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val protocol = AiProtocol.fromName(item.optString("protocol"))
            AiModelProfile(
                id = id,
                name = item.optString("name").ifBlank { id },
                protocol = protocol,
                endpoint = item.optString("endpoint").ifBlank { protocol.defaultEndpoint },
                apiKey = item.optString("apiKey"),
                model = item.optString("model"),
                maxTokens = item.optInt("maxTokens", 800).coerceIn(64, 8192),
                maxOutputCharacters = item.optInt("maxOutputCharacters", 1200).coerceIn(100, 10000),
                context = item.optString("context").ifBlank { DEFAULT_AI_CONTEXT },
                rawRequestJson = item.optString("rawRequestJson").ifBlank { protocol.defaultRequestJson },
            )
        }.distinctBy { it.id }
        val selectedId = root.optString("selectedId").takeIf { id -> models.any { it.id == id } }
            ?: models.firstOrNull()?.id.orEmpty()
        return AiProfiles(models, selectedId)
    }

    private fun modelJson(it: AiModelProfile) = JSONObject()
        .put("id", it.id).put("name", it.name).put("protocol", it.protocol.name)
        .put("endpoint", it.endpoint).put("apiKey", it.apiKey).put("model", it.model)
        .put("maxTokens", it.maxTokens).put("maxOutputCharacters", it.maxOutputCharacters)
        .put("context", it.context).put("rawRequestJson", it.rawRequestJson)

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
        (0 until length()).mapNotNull(::optJSONObject)

    private companion object {
        const val KEY = "aiProfiles"
        const val MIGRATED_ID = "migrated-ai"
    }
}
