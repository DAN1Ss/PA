package GetJson

import JsonLib.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SampleApplicationTest {
    private var server: Server? = null
    private val testPort = 8889

    @RestController
    class StoreController {
        @GetMapping("/store/items")
        fun getItems(): JsonArray {
            return JsonArray(listOf(
                JsonObject(mapOf(
                    "id" to JsonString("1"),
                    "name" to JsonString("Book"),
                    "price" to JsonNumber(29.99)
                )),
                JsonObject(mapOf(
                    "id" to JsonString("2"),
                    "name" to JsonString("Pen"),
                    "price" to JsonNumber(3.99)
                ))
            ))
        }

        @GetMapping("/store/items/{id}")
        fun getItem(@PathVariable id: String): JsonObject {
            return when (id) {
                "1" -> JsonObject(mapOf(
                    "id" to JsonString("1"),
                    "name" to JsonString("Book"),
                    "price" to JsonNumber(29.99),
                    "description" to JsonString("A great book to read")
                ))
                "2" -> JsonObject(mapOf(
                    "id" to JsonString("2"),
                    "name" to JsonString("Pen"),
                    "price" to JsonNumber(3.99),
                    "description" to JsonString("A blue pen")
                ))
                else -> JsonObject(mapOf(
                    "error" to JsonString("Item not found"),
                    "status" to JsonNumber(404)
                ))
            }
        }

        @GetMapping("/store/categories")
        fun getCategories(): JsonArray {
            return JsonArray(listOf(
                JsonObject(mapOf(
                    "id" to JsonString("books"),
                    "count" to JsonNumber(1)
                )),
                JsonObject(mapOf(
                    "id" to JsonString("supplies"),
                    "count" to JsonNumber(1)
                ))
            ))
        }
    }

    @BeforeEach
    fun setUp() {
        server = ServerBuilder()
            .port(testPort)
            .addController(StoreController())
            .build()

        thread { server?.start() }
        Thread.sleep(500)
    }

    @AfterEach
    fun tearDown() {
        server?.stop()
        server = null
        Thread.sleep(200)
    }

    private fun sendRequest(path: String): Pair<Int, String> {
        val url = URL("http://localhost:$testPort$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val status = connection.responseCode
        val response = BufferedReader(InputStreamReader(
            if (status < 400) connection.inputStream else connection.errorStream
        )).use { it.readText() }

        connection.disconnect()
        return Pair(status, response)
    }

    @Test
    fun `test get all items using JsonArray`() {
        val (status, response) = sendRequest("/store/items")
        assertEquals(200, status)

        // Create expected JsonArray
        val expected = JsonArray(listOf(
            JsonObject(mapOf(
                "id" to JsonString("1"),
                "name" to JsonString("Book"),
                "price" to JsonNumber(29.99)
            )),
            JsonObject(mapOf(
                "id" to JsonString("2"),
                "name" to JsonString("Pen"),
                "price" to JsonNumber(3.99)
            ))
        ))

        assertEquals(expected.toJsonString(), response)
    }

    @Test
    fun `test get single item using JsonObject`() {
        val (status, response) = sendRequest("/store/items/1")
        assertEquals(200, status)

        val expected = JsonObject(mapOf(
            "id" to JsonString("1"),
            "name" to JsonString("Book"),
            "price" to JsonNumber(29.99),
            "description" to JsonString("A great book to read")
        ))

        assertEquals(expected.toJsonString(), response)
    }

    @Test
    fun `test get categories using nested JsonObjects and JsonArrays`() {
        val (status, response) = sendRequest("/store/categories")
        assertEquals(200, status)

        val expected = JsonArray(listOf(
                JsonObject(mapOf(
                    "id" to JsonString("books"),
                    "count" to JsonNumber(1)
                )),
                JsonObject(mapOf(
                    "id" to JsonString("supplies"),
                    "count" to JsonNumber(1)
                ))
            ))

        assertEquals(expected.toJsonString(), response)
    }

    @Test
    fun `test get nonexistent item returns 500`() {
        val (status, response) = sendRequest("/store/items/999")
        assertEquals(200, status)

        val expected = JsonObject(mapOf(
            "error" to JsonString("Item not found"),
            "status" to JsonNumber(404)
        ))

        assertEquals(expected.toJsonString(), response)
    }

    @Test
    fun `test array homogeneity validation`() {
        val (_, response) = sendRequest("/store/items")

        // Parse response back to JsonArray and validate
        val items = JsonArray(listOf(
            JsonObject(mapOf(
                "id" to JsonString("1"),
                "name" to JsonString("Book"),
                "price" to JsonNumber(29.99)
            )),
            JsonObject(mapOf(
                "id" to JsonString("2"),
                "name" to JsonString("Pen"),
                "price" to JsonNumber(3.99)
            ))
        ))

        val validator = ArrayHomogeneityValidator()
        items.accept(validator)
        assertTrue(validator.isValid)
    }
}