object JsonNull : JsonValue() {
    override fun toJsonString(): String = "null"

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)
}
