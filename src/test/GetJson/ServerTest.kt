package GetJson


import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ServerTest {
    private var server: Server? = null
    private val testPort = 8888

    @RestController
    class TestController {
        @GetMapping("/test")
        fun test(): String {
            return "Hello, World!"
        }

        @GetMapping("/json")
        fun jsonTest(): Map<String, Any> {
            return mapOf("key" to "value", "number" to 42)
        }

        @GetMapping("/users/{id}")
        fun getUser(@PathVariable id: String): Map<String, Any> {
            return mapOf("id" to id, "name" to "User $id")
        }

        @GetMapping("/search")
        fun search(@RequestParam query: String): Map<String, Any> {
            return mapOf("query" to query, "results" to listOf("result1", "result2"))
        }

        @GetMapping("/optional")
        fun optional(@RequestParam(required = false) param: String?): Map<String, String?> {
            return mapOf("param" to param)
        }

        @GetMapping("/types")
        fun types(
            @RequestParam intValue: Int,
            @RequestParam boolValue: Boolean,
            @RequestParam doubleValue: Double
        ): Map<String, Any> {
            return mapOf(
                "intValue" to intValue,
                "boolValue" to boolValue,
                "doubleValue" to doubleValue
            )
        }
    }

    @BeforeEach
    fun setUp() {
        val controller = TestController()
        server = ServerBuilder()
            .port(testPort)
            .addController(controller)
            .build()

        // Start server in a separate thread
        thread {
            server?.start()
        }

        // Give the server time to start
        Thread.sleep(500)
    }

    @AfterEach
    fun tearDown() {
        server?.stop()
        server = null
        // Give the server more time to fully release the port
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
    fun `test basic endpoint`() {
        val (status, response) = sendRequest("/test")
        assertEquals(200, status)
        assertEquals("\"Hello, World!\"", response)
    }

    @Test
    fun `test JSON object response`() {
        val (status, response) = sendRequest("/json")
        assertEquals(200, status)
        assertTrue(response.contains("\"key\":\"value\""))
        assertTrue(response.contains("\"number\":42"))
    }

    @Test
    fun `test path variable`() {
        val (status, response) = sendRequest("/users/123")
        assertEquals(200, status)
        assertTrue(response.contains("\"id\":\"123\""))
        assertTrue(response.contains("\"name\":\"User 123\""))
    }

    @Test
    fun `test request parameter`() {
        val (status, response) = sendRequest("/search?query=kotlin")
        assertEquals(200, status)
        assertTrue(response.contains("\"query\":\"kotlin\""))
        assertTrue(response.contains("\"results\":[\"result1\",\"result2\"]"))
    }

    @Test
    fun `test optional parameter present`() {
        val (status, response) = sendRequest("/optional?param=test")
        assertEquals(200, status)
        assertTrue(response.contains("\"param\":\"test\""))
    }

    @Test
    fun `test optional parameter absent`() {
        val (status, response) = sendRequest("/optional")
        assertEquals(200, status)
        assertTrue(response.contains("\"param\":null"))
    }

    @Test
    fun `test different parameter types`() {
        val (status, response) = sendRequest("/types?intValue=42&boolValue=true&doubleValue=3.14")
        assertEquals(200, status)
        assertTrue(response.contains("\"intValue\":42"))
        assertTrue(response.contains("\"boolValue\":true"))
        assertTrue(response.contains("\"doubleValue\":3.14"))
    }

    @Test
    fun `test missing required parameter`() {
        val (status, _) = sendRequest("/search")
        assertEquals(500, status) // Server returns 500 when parameter binding fails
    }

    @Test
    fun `test nonexistent endpoint`() {
        val (status, _) = sendRequest("/nonexistent")
        assertEquals(404, status)
    }
}