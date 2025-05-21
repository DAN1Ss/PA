package JsonLib

sealed class JsonValue {
    abstract fun toJsonString(): String

    // Permite visitantes no futuro
    abstract fun accept(visitor: JsonVisitor)
}