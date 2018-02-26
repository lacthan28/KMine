package kmine.command

import kmine.Server
import kmine.lang.TextContainer
import kmine.permission.Permissible

interface CommandSender : Permissible {
    fun sendMessage(message: String)
    fun sendMessage(message: TextContainer)
    fun getServer(): Server
    fun getName(): String

    fun isPlayer(): Boolean
}