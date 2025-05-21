package JsonLib

class ObjectValidator : JsonVisitor {
    var isValid = true
    private val keys = mutableSetOf<String>()

    override fun visit(value: JsonObject) {
        value.properties.forEach { (k, v) ->
            if (k in keys || k.isBlank()) {
                isValid = false
            } else {
                keys.add(k)
                v.accept(this)
            }
        }
    }

    override fun visit(value: JsonArray) {
        value.elements.forEach { it.accept(this) }
    }

    override fun visit(value: JsonString) {}
    override fun visit(value: JsonNumber) {}
    override fun visit(value: JsonBoolean) {}
    override fun visit(value: JsonNull) {}
}
