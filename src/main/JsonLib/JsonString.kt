package JsonLib

data class JsonString(val value: String) : JsonValue() {
    override fun toJsonString(): String = "\"${value.replace("\"", "\\\"")}\""

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

}