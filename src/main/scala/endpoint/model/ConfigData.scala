package endpoint.model

case class ConfigData(server: Server = Server())
case class Server(host: String = "localhost", port: Int = 8000)
