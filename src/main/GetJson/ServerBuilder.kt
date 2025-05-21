package GetJson

class ServerBuilder {
    private var port: Int = 8080
    private val controllers = mutableListOf<Any>()

    fun port(port: Int): ServerBuilder {
        this.port = port
        return this
    }

    fun addController(controller: Any): ServerBuilder {
        controllers.add(controller)
        return this
    }

    fun build(): Server {
        return Server(port, controllers)
    }
}