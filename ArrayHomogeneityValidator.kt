class ArrayHomogeneityValidator : JsonVisitor {
    var isValid = true

    override fun visit(value: JsonArray) {
        val types = value.elements
            .filterNot { it is JsonNull }
            .map { it::class }
            .distinct()

        if (types.size > 1) isValid = false
    }

    override fun visit(value: JsonObject) {
        value.properties.values.forEach { it.accept(this) }
    }

    override fun visit(value: JsonString) {}
    override fun visit(value: JsonNumber) {}
    override fun visit(value: JsonBoolean) {}
    override fun visit(value: JsonNull) {}
}
