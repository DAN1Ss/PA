package JsonLib

interface JsonVisitor {
    fun visit(value: JsonString) {}
    fun visit(value: JsonNumber)
    fun visit(value: JsonBoolean)
    fun visit(value: JsonNull)
    fun visit(value: JsonArray)
    fun visit(value: JsonObject)
}
