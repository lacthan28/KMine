package kmine.command

import com.sun.xml.internal.fastinfoset.util.StringArray

interface CommandExecutor {
    fun onCommand(sender: CommandSender, command: Command, label:String, args: StringArray):Boolean
}