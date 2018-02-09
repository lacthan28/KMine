package kmine.permission

import kmine.Server
import kmine.utils.writeln
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BanList(private var file: String) {
    var list: Map<String, BanEntry> = emptyMap()
        get() {
            removeExpired()
            return field
        }

    var isEnabled = true

    fun getEntry(name: String): BanEntry? {
        removeExpired()
        return list[name.toLowerCase()]
    }

    fun isBanned(name: String): Boolean {
        if (!isEnabled) return false
        else
            removeExpired()
        return list[name.toLowerCase()] != null
    }

    fun add(entry: BanEntry) {
        list.getOrElse(entry.name, { this.add(entry) })
        save()
    }

    fun addBan(target: String, reason: String? = null, expires: Date? = null, source: String? = null): BanEntry {
        val entry = BanEntry(target)
        entry.source = source ?: entry.source
        entry.expirationDate = expires
        entry.reason = reason ?: entry.reason

        list.getOrElse(entry.name, { this.add(entry) })
        save()

        return entry
    }

    fun remove(name: String) {
        val lowerName = name.toLowerCase()
        if (list.containsKey(lowerName)) {
            list.toMutableMap().remove(lowerName)
            save()
        }
    }

    fun removeExpired() {
        list.forEach {
            if (it.value.hasExpired()) list.toMutableMap().remove(it.key, it.value)
        }
    }

    fun load() {
        list = emptyMap()
        if (File(file).isFile) {
            File(file).bufferedReader().use {
                if (!it.readLine().startsWith("#")) {
                    val entry = BanEntry.fromString(it.readLine())
                    if (entry is BanEntry) {
                        list.getOrElse(entry.name, { this.add(entry) })
                    }
                }
            }
        } else {
            //Log because cannot load ban list
        }
    }

    fun save(flag: Boolean = true) {
        removeExpired()
        if (File(file).isFile) {
            File(file).printWriter().use { printWriter ->
                {
                    if (flag) {
                        val format = SimpleDateFormat("MM/dd/YY HH:mm")
                        printWriter.writeln("# Updated ${format.format(Date())} by ${Server.instance.name} ${Server.instance.version}")
                        printWriter.writeln("# victim name | ban date | banned by | banned until | reason \n")
                    }

                    list.forEach {
                        printWriter.writeln(it.value.getString())
                    }
                }
            }
        } else {
            // Could not save ban list
        }
    }
}