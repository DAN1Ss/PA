data class JsonObject(val properties: Map<String, JsonValue>) : JsonValue() {

    fun filter(predicate: (Map.Entry<String, JsonValue>) -> Boolean): JsonObject =
        JsonObject(properties.filter(predicate))

    override fun toJsonString(): String =
        properties.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\":${it.value.toJsonString()}"
        }

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)
}
