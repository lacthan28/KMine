package kmine

import kmine.permission.BanList

class Server {
    val BROADCAST_CHANNEL_ADMINISTRATIVE = "pocketmine.broadcast.admin"
    val BROADCAST_CHANNEL_USERS = "pocketmine.broadcast.user"

    val name = KMine.NAME
    val version = KMine.VERSION

    private var banByName: BanList? = null
    private var banByIP: BanList? = null

    companion object {
        val instance: Server by lazy {
            Server()
        }

        val sleeper: Thread? = null
    }


}
