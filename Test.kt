import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Test {

    @Test
    fun `deve criar e serializar valores JSON básicos`() {
        val s = JsonString("Hello")
        val n = JsonNumber(42)
        val b = JsonBoolean(true)
        val nullVal = JsonNull

        assertEquals("\"Hello\"", s.toJsonString())
        assertEquals("42", n.toJsonString())
        assertEquals("true", b.toJsonString())
        assertEquals("null", nullVal.toJsonString())
    }

    @Test
    fun `deve criar e serializar arrays JSON`() {
        val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2)))
        assertEquals("[1,2]", array.toJsonString())
    }

    @Test
    fun `deve criar e serializar objetos JSON`() {
        val obj = JsonObject(mapOf(
            "nome" to JsonString("Ana"),
            "idade" to JsonNumber(30)
        ))

        val output = obj.toJsonString()
        assertTrue(output.contains("\"nome\":\"Ana\""))
        assertTrue(output.contains("\"idade\":30"))
    }

    @Test
    fun `filter em JsonArray deve funcionar corretamente`() {
        val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2), JsonString("a")))
        val apenasNumeros = array.filter { it is JsonNumber }

        assertEquals(2, apenasNumeros.elements.size)
        assertTrue(apenasNumeros.elements.all { it is JsonNumber })
    }

    @Test
    fun `map em JsonArray deve transformar corretamente`() {
        val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2)))
        val duplicados = array.map {
            if (it is JsonNumber) JsonNumber(it.value.toInt() * 2) else it
        }

        assertEquals("[2,4]", duplicados.toJsonString())
    }

    @Test
    fun `filter em JsonObject deve filtrar corretamente`() {
        val obj = JsonObject(mapOf(
            "nome" to JsonString("Ana"),
            "idade" to JsonNumber(30)
        ))
        val soIdade = obj.filter { it.key == "idade" }

        assertEquals("{\"idade\":30}", soIdade.toJsonString())
    }

    @Test
    fun `ObjectValidator deve validar objetos com chaves únicas e válidas`() {
        val objValido = JsonObject(mapOf("nome" to JsonString("João")))
        val visitor = ObjectValidator()
        objValido.accept(visitor)
        assertTrue(visitor.isValid)

        val objInvalido = JsonObject(mapOf("" to JsonString("erro")))
        val visitor2 = ObjectValidator()
        objInvalido.accept(visitor2)
        assertFalse(visitor2.isValid)
    }

    @Test
    fun `ArrayHomogeneityValidator deve validar arrays homogêneos`() {
        val arrayHomogeneo = JsonArray(listOf(JsonNumber(1), JsonNumber(2)))
        val visitor = ArrayHomogeneityValidator()
        arrayHomogeneo.accept(visitor)
        assertTrue(visitor.isValid)

        val arrayMisturado = JsonArray(listOf(JsonNumber(1), JsonString("x")))
        val visitor2 = ArrayHomogeneityValidator()
        arrayMisturado.accept(visitor2)
        assertFalse(visitor2.isValid)
    }
}
