data class JsonNumber(val value: Number) : JsonValue() {
    override fun toJsonString(): String {
        val d = value.toDouble()
        if (d.isNaN() || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            throw IllegalArgumentException("Invalid JSON number: $value")
        }
        return value.toString()
    }

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)
}
