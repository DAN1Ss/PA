import kotlin.reflect.full.declaredMemberProperties

object ModelBuilder {
    fun buildModel(value: Any?): JsonValue = when (value) {
        null -> JsonNull
        is Int -> JsonNumber(value)
        is Double -> JsonNumber(value)
        is Boolean -> JsonBoolean(value)
        is String -> JsonString(value)
        is Enum<*> -> JsonString(value.name)
        is List<*> -> JsonArray(value.map { buildModel(it) })
        is Map<*, *> -> {
            require(value.keys.all { it is String }) { "Map keys must be strings" }
            JsonObject(value.mapKeys { it.key as String }
                .mapValues { buildModel(it.value) })
        }
        else -> {
            if (value::class.isData) {
                val properties = value::class
                    .declaredMemberProperties
                    .associate { prop ->
                        prop.name to buildModel(prop.getter.call(value))
                    }
                JsonObject(properties)
            } else {
                throw IllegalArgumentException("Unsupported type: ${value::class}")
            }
        }
    }
}