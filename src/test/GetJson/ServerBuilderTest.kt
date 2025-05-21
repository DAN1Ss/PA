package GetJson

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ServerBuilderTest {

    @RestController
    class MinimalController {
        @GetMapping("/ping")
        fun ping(): String = "pong"
    }

    @Test
    fun `build server with default port`() {
        val controller = MinimalController()
        val server = ServerBuilder()
            .addController(controller)
            .build()

        // Verify server was built with the default port (8080)
        assertEquals(8080, getServerPort(server))
    }

    @Test
    fun `build server with custom port`() {
        val customPort = findAvailablePort()
        val controller = MinimalController()
        val server = ServerBuilder()
            .port(customPort)
            .addController(controller)
            .build()

        // Verify server was built with the custom port
        assertEquals(customPort, getServerPort(server))
    }

    @Test
    fun `build server with multiple controllers`() {
        val controller1 = MinimalController()
        val controller2 = MinimalController()
        val server = ServerBuilder()
            .addController(controller1)
            .addController(controller2)
            .build()

        // Verify server contains both controllers
        val controllers = getServerControllers(server)
        assertEquals(2, controllers.size)
        assertTrue(controllers.contains(controller1))
        assertTrue(controllers.contains(controller2))
    }

    @Test
    fun `builder has fluent interface`() {
        val controller = MinimalController()
        val builder = ServerBuilder()

        // Verify that methods return the builder for chaining
        assertSame(builder, builder.port(9000))
        assertSame(builder, builder.addController(controller))
    }

    @Test
    fun `server can start and stop`() {
        val port = findAvailablePort()
        val controller = MinimalController()
        val server = ServerBuilder()
            .port(port)
            .addController(controller)
            .build()

        val running = AtomicBoolean(true)

        val serverThread = thread {
            server.start()
            running.set(false)
        }

        // Give the server time to start
        Thread.sleep(500)

        // Verify the port is in use
        assertThrows(Exception::class.java) {
            ServerSocket(port).use { /* This should fail if server is running */ }
        }

        // Stop the server
        server.stop()

        // Wait for the server thread to complete
        serverThread.join(1000)

        // Verify the server actually stopped
        assertFalse(running.get())

        // Verify the port is now available
        ServerSocket(port).use { /* This should succeed if server stopped */ }
    }

    // Helper methods to access private fields (using reflection)
    private fun getServerPort(server: Server): Int {
        val field = Server::class.java.getDeclaredField("port")
        field.isAccessible = true
        return field.getInt(server)
    }

    private fun getServerControllers(server: Server): List<Any> {
        val field = Server::class.java.getDeclaredField("controllers")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(server) as List<Any>
    }

    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
}