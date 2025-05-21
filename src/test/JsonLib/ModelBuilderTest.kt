package JsonLib

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test





class ModelBuilderTest {

    enum class Color { RED, GREEN, BLUE }

    @Test
    fun `should convert simple Kotlin values to JSON model`() {
        // Basic types
        assertEquals(JsonNumber(42), ModelBuilder.buildModel(42))
        assertEquals(JsonNumber(3.14), ModelBuilder.buildModel(3.14))
        assertEquals(JsonBoolean(true), ModelBuilder.buildModel(true))
        assertEquals(JsonString("hello"), ModelBuilder.buildModel("hello"))
        assertEquals(JsonNull, ModelBuilder.buildModel(null))
    }

    @Test
    fun `should convert enum values to JSON strings`() {

        // JsonLib.Test conversion of enum values
        assertEquals(JsonString("RED"), ModelBuilder.buildModel(Color.RED))
        assertEquals(JsonString("GREEN"), ModelBuilder.buildModel(Color.GREEN))
        assertEquals(JsonString("BLUE"), ModelBuilder.buildModel(Color.BLUE))

        // JsonLib.Test enum in a data class
        data class ColorData(val favoriteColor: Color)
        val colorData = ColorData(Color.BLUE)

        val jsonValue = ModelBuilder.buildModel(colorData)
        assertTrue(jsonValue is JsonObject)
        val jsonObject = jsonValue as JsonObject

        assertEquals(JsonString("BLUE"), jsonObject.properties["favoriteColor"])
    }

    @Test
    fun `should convert Kotlin lists to JSON arrays`() {
        val list = listOf(1, 2, 3)
        val jsonValue = ModelBuilder.buildModel(list)

        assertTrue(jsonValue is JsonArray)
        val jsonArray = jsonValue as JsonArray
        assertEquals(3, jsonArray.elements.size)
        assertTrue(jsonArray.elements.all { it is JsonNumber })
    }

    @Test
    fun `should convert Kotlin maps with string keys to JSON objects`() {
        val map = mapOf("name" to "John", "age" to 30)
        val jsonValue = ModelBuilder.buildModel(map)

        assertTrue(jsonValue is JsonObject)
        val jsonObject = jsonValue as JsonObject
        assertEquals(2, jsonObject.properties.size)
        assertEquals(JsonString("John"), jsonObject.properties["name"])
        assertEquals(JsonNumber(30), jsonObject.properties["age"])
    }

    @Test
    fun `should convert data classes to JSON objects`() {
        data class Person(val name: String, val age: Int)

        val person = Person("Alice", 25)
        val jsonValue = ModelBuilder.buildModel(person)

        assertTrue(jsonValue is JsonObject)
        val jsonObject = jsonValue as JsonObject
        assertEquals(2, jsonObject.properties.size)
        assertEquals(JsonString("Alice"), jsonObject.properties["name"])
        assertEquals(JsonNumber(25), jsonObject.properties["age"])
    }

    @Test
    fun `should handle nested complex objects`() {
        data class Address(val street: String, val city: String)
        data class Person(
            val name: String,
            val address: Address,
            val hobbies: List<String>,
            val metadata: Map<String, Any?>
        )

        val person = Person(
            name = "Bob",
            address = Address("123 Main St", "Springfield"),
            hobbies = listOf("Reading", "Coding"),
            metadata = mapOf("active" to true, "notes" to null)
        )

        val jsonValue = ModelBuilder.buildModel(person)

        assertTrue(jsonValue is JsonObject)
        val jsonObject = jsonValue as JsonObject

        // Verify top-level properties
        assertEquals(JsonString("Bob"), jsonObject.properties["name"])

        // Verify nested data class
        val address = jsonObject.properties["address"]
        assertTrue(address is JsonObject)
        assertEquals(JsonString("123 Main St"), (address as JsonObject).properties["street"])
        assertEquals(JsonString("Springfield"), address.properties["city"])

        // Verify list
        val hobbies = jsonObject.properties["hobbies"]
        assertTrue(hobbies is JsonArray)
        assertEquals(2, (hobbies as JsonArray).elements.size)

        // Verify map with mixed values
        val metadata = jsonObject.properties["metadata"]
        assertTrue(metadata is JsonObject)
        assertEquals(JsonBoolean(true), (metadata as JsonObject).properties["active"])
        assertEquals(JsonNull, metadata.properties["notes"])
    }
}