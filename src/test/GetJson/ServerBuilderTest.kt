package GetJson

import JsonLib.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ServerBuilderTest {

    @RestController
    class TestController {
        @GetMapping("/test")
        fun test(): JsonObject {
            return JsonObject(mapOf(
                "message" to JsonString("test"),
                "status" to JsonNumber(200)
            ))
        }
    }

    @Test
    fun `test server builder with default port`() {
        val server = ServerBuilder()
            .addController(TestController())
            .build()

        val port = getServerPort(server)
        assertEquals(8080, port)
    }

    @Test
    fun `test server builder with custom port`() {
        val server = ServerBuilder()
            .port(9000)
            .addController(TestController())
            .build()

        val port = getServerPort(server)
        assertEquals(9000, port)
        assertFalse(port==8080);
    }

    @Test
    fun `test server builder with controller`() {
        val controller = TestController()
        val server = ServerBuilder()
            .addController(controller)
            .build()

        val controllers = getServerControllers(server)
        assertTrue(controllers.contains(controller))
    }

    // Helper method to access private port field
    private fun getServerPort(server: Server): Int {
        val field = Server::class.java.getDeclaredField("port")
        field.isAccessible = true
        return field.getInt(server)
    }

    // Helper method to access private controllers field
    private fun getServerControllers(server: Server): List<Any> {
        val field = Server::class.java.getDeclaredField("controllers")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(server) as List<Any>
    }
}