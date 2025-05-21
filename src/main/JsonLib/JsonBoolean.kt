package JsonLib

data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun toJsonString(): String = value.toString()

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)
}
