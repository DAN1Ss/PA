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

class SampleApplicationTest {
    private var server: Server? = null
    private val testPort = 8889

    // Sample data models
    data class Product(val id: String, val name: String, val price: Double)

    // Sample controller
    @RestController
    class ProductController {
        private val products = mapOf(
            "1" to Product("1", "Laptop", 999.99),
            "2" to Product("2", "Smartphone", 499.99),
            "3" to Product("3", "Headphones", 99.99)
        )

        @GetMapping("/products")
        fun getAllProducts(): List<Product> {
            return products.values.toList()
        }

        @GetMapping("/products/{id}")
        fun getProduct(@PathVariable id: String): Product? {
            return products[id]
        }

        @GetMapping("/products/search")
        fun searchProducts(
            @RequestParam(required = false) minPrice: Double?,
            @RequestParam(required = false) maxPrice: Double?
        ): List<Product> {
            return products.values.filter { product ->
                (minPrice == null || product.price >= minPrice) &&
                        (maxPrice == null || product.price <= maxPrice)
            }
        }
    }

    @BeforeEach
    fun setUp() {
        val controller = ProductController()
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
        server = null  // Set to null to aid garbage collection
        Thread.sleep(200)  // Allow more time for the server socket to close
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
    fun `get all products`() {
        val (status, response) = sendRequest("/products")
        assertEquals(200, status)

        // Verify all products are returned
        assertTrue(response.contains("\"id\":\"1\""))
        assertTrue(response.contains("\"name\":\"Laptop\""))
        assertTrue(response.contains("\"price\":999.99"))

        assertTrue(response.contains("\"id\":\"2\""))
        assertTrue(response.contains("\"id\":\"3\""))
    }

    @Test
    fun `get product by id`() {
        val (status, response) = sendRequest("/products/2")
        assertEquals(200, status)

        // Verify the correct product is returned
        assertTrue(response.contains("\"id\":\"2\""))
        assertTrue(response.contains("\"name\":\"Smartphone\""))
        assertTrue(response.contains("\"price\":499.99"))

        // Verify other products are not returned
        assertFalse(response.contains("\"id\":\"1\""))
        assertFalse(response.contains("\"id\":\"3\""))
    }

    @Test
    fun `search products by price range`() {
        val (status, response) = sendRequest("/products/search?minPrice=100&maxPrice=500")
        assertEquals(200, status)
        println(response)

        // Verify only products in the price range are returned
        assertTrue(response.contains("\"id\":\"2\""))      // Smartphone at 499.99
        assertFalse(response.contains("\"id\":\"1\""))     // Laptop at 999.99 (too expensive)
        assertFalse(response.contains("\"id\":\"3\""))     // Headphones at 99.99 (too cheap)
    }

    @Test
    fun `search products with only min price`() {
        val (status, response) = sendRequest("/products/search?minPrice=500")
        assertEquals(200, status)

        // Verify only products above the min price are returned
        assertTrue(response.contains("\"id\":\"1\""))      // Laptop at 999.99
        assertTrue(response.contains("\"id\":\"2\""))      // Smartphone at 499.99
        assertFalse(response.contains("\"id\":\"3\""))     // Headphones at 99.99
    }

    @Test
    fun `search products with only max price`() {
        val (status, response) = sendRequest("/products/search?maxPrice=100")
        assertEquals(200, status)

        // Verify only products below the max price are returned
        assertFalse(response.contains("\"id\":\"1\""))     // Laptop at 999.99
        assertFalse(response.contains("\"id\":\"2\""))     // Smartphone at 499.99
        assertTrue(response.contains("\"id\":\"3\""))      // Headphones at 99.99
    }

    @Test
    fun `search products with no parameters returns all products`() {
        val (status, response) = sendRequest("/products/search")
        assertEquals(200, status)

        // Verify all products are returned
        assertTrue(response.contains("\"id\":\"1\""))
        assertTrue(response.contains("\"id\":\"2\""))
        assertTrue(response.contains("\"id\":\"3\""))
    }
}