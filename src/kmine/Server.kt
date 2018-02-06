package kmine

class Server {
    val BROADCAST_CHANNEL_ADMINISTRATIVE = "pocketmine.broadcast.admin"
    val BROADCAST_CHANNEL_USERS = "pocketmine.broadcast.user"

    companion object {
        val instance: Server by lazy {
            Server()
        }

        val sleeper: Thread? = null
    }

    private var banByName: BanList? = null
}
