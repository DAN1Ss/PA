package GetJson

import JsonLib.ModelBuilder
import JsonLib.JsonValue
import java.net.ServerSocket
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaType

class Server(private val port: Int, private val controllers: List<Any>) {
    private val routes = mutableMapOf<RouteInfo, EndpointInfo>()
    private var running = false
    private var serverSocket: ServerSocket? = null


    init {
        registerControllers()
    }

    private fun registerControllers() {
        controllers.forEach { controller ->
            val controllerClass = controller::class
            if (!controllerClass.hasAnnotation<RestController>()) {
                throw IllegalArgumentException("Class ${controllerClass.simpleName} is not annotated with @RestController")
            }

            controllerClass.functions.filter { it.hasAnnotation<GetMapping>() }.forEach { function ->
                val mapping = function.findAnnotation<GetMapping>()!!
                val pathPattern = mapping.path
                val pathSegments = pathPattern.split("/").filter { it.isNotEmpty() }

                val routeInfo = RouteInfo(pathSegments)
                val endpointInfo = EndpointInfo(controller, function)

                routes[routeInfo] = endpointInfo
            }
        }
    }

    fun start() {
        if (running) return
        running = true

        serverSocket = ServerSocket(port)
        println("Server started on port $port")

        while (running) {
            try {
                val socket = serverSocket?.accept() ?: break
                Thread {
                    try {
                        val reader = socket.getInputStream().bufferedReader()
                        val writer = socket.getOutputStream().bufferedWriter()

                        val requestLine = reader.readLine()
                        if (requestLine != null) {
                            val parts = requestLine.split(" ")
                            if (parts.size >= 2 && parts[0] == "GET") {
                                val urlAndParams = parts[1].split("?", limit = 2)
                                val path = urlAndParams[0]
                                val queryParams = if (urlAndParams.size > 1) parseQueryParams(urlAndParams[1]) else emptyMap()

                                handleRequest(path, queryParams, writer)
                            }
                        }

                        reader.close()
                        writer.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            } catch (e: Exception) {
                if (!running) break // Normal termination
                e.printStackTrace()
            }
            closeServerSocket()
        }
    }



    private fun handleRequest(path: String, queryParams: Map<String, String>, writer: java.io.BufferedWriter) {
        val pathSegments = path.split("/").filter { it.isNotEmpty() }

        for ((routeInfo, endpointInfo) in routes) {
            val pathVariables = matchRoute(routeInfo.pathSegments, pathSegments)
            if (pathVariables != null) {
                try {
                    val args = bindArguments(endpointInfo.function, pathVariables, queryParams)
                    val result = endpointInfo.function.call(endpointInfo.controller, *args.toTypedArray())

                    val jsonValue = if (result is JsonValue) result else ModelBuilder.buildModel(result)

                    // Write HTTP response
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Type: application/json\r\n")
                    writer.write("\r\n")
                    writer.write(jsonValue.toJsonString())
                    writer.flush()
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                    writer.write("HTTP/1.1 500 Internal Server Error\r\n\r\n")
                    writer.flush()
                    return
                }
            }
        }

        writer.write("HTTP/1.1 404 Not Found\r\n\r\n")
        writer.flush()
    }

    private fun matchRoute(routePatternSegments: List<String>, pathSegments: List<String>): Map<String, String>? {
        if (routePatternSegments.size != pathSegments.size) {
            return null
        }

        val pathVariables = mutableMapOf<String, String>()

        for (i in routePatternSegments.indices) {
            val patternSegment = routePatternSegments[i]
            val pathSegment = pathSegments[i]

            if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
                val varName = patternSegment.substring(1, patternSegment.length - 1)
                pathVariables[varName] = pathSegment
            } else if (patternSegment != pathSegment) {
                return null
            }
        }

        return pathVariables
    }

    private fun bindArguments(function: KFunction<*>, pathVariables: Map<String, String>, queryParams: Map<String, String>): List<Any?> {
        val args = mutableListOf<Any?>()

        function.parameters.forEach { param ->
            if (param.kind == KParameter.Kind.INSTANCE) {
                return@forEach // Skip 'this' parameter
            }

            val pathVarAnnotation = param.findAnnotation<PathVariable>()
            if (pathVarAnnotation != null) {
                val name = if (pathVarAnnotation.name.isNotEmpty()) pathVarAnnotation.name else param.name!!
                val value = pathVariables[name] ?: throw IllegalArgumentException("Missing path variable: $name")
                args.add(convertValue(value, param.type.javaType))
                return@forEach
            }

            val requestParamAnnotation = param.findAnnotation<RequestParam>()
            if (requestParamAnnotation != null) {
                val name = if (requestParamAnnotation.name.isNotEmpty()) requestParamAnnotation.name else param.name!!
                val value = queryParams[name]
                if (value == null && requestParamAnnotation.required) {
                    throw IllegalArgumentException("Missing required query parameter: $name")
                }
                args.add(if (value != null) convertValue(value, param.type.javaType) else null)
                return@forEach
            }

            // If no annotation, treat as a required request parameter with the parameter name
            val value = queryParams[param.name]
            if (value == null) {
                throw IllegalArgumentException("Missing required parameter: ${param.name}")
            }
            args.add(convertValue(value, param.type.javaType))
        }

        return args
    }

    private fun convertValue(value: String, type: java.lang.reflect.Type): Any {
        return when (type) {
            String::class.java -> value
            Int::class.java -> value.toInt()
            Long::class.java -> value.toLong()
            Double::class.java -> value.toDouble()
            Float::class.java -> value.toFloat()
            Boolean::class.java -> value.toBoolean()
            else -> throw IllegalArgumentException("Unsupported parameter type: $type")
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        return queryString.split("&")
            .filter { it.isNotEmpty() }
            .associate {
                val parts = it.split("=", limit = 2)
                val key = parts[0]
                val value = if (parts.size > 1) parts[1] else ""
                key to value
            }
    }

    fun stop() {
        running = false
        closeServerSocket()
    }

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class RouteInfo(val pathSegments: List<String>)

    private data class EndpointInfo(
        val controller: Any,
        val function: KFunction<*>
    )
}