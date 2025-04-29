data class JsonArray(val elements: List<JsonValue>) : JsonValue() {

    fun filter(predicate: (JsonValue) -> Boolean): JsonArray =
        JsonArray(elements.filter(predicate))

    fun map(transform: (JsonValue) -> JsonValue): JsonArray =
        JsonArray(elements.map(transform))

    override fun toJsonString(): String =
        elements.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJsonString() }

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)
}
