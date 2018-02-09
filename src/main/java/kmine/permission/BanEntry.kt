package kmine.permission

import kmine.utils.createDateFromFormat
import kmine.utils.pop
import java.text.SimpleDateFormat
import java.util.*

class BanEntry(name: String) {
    companion object {
        val format: SimpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z") }

        fun fromString(str: String): BanEntry? {
            if (str.length < 2) return null
            val strings = str.split("|", str.trim())
            val entry = BanEntry(strings.pop().trim())
            if (strings.count() > 0) {
                val datetime = Date().createDateFromFormat(format)
                entry.creationDate = datetime
                if (strings.count() > 0) {
                    entry.source = strings.pop().trim()
                    if (strings.count() > 0) {
                        val expire = strings.pop().trim()
                        if ((expire.toLowerCase() != "forever") and (expire.isNotEmpty()))
                            entry.expirationDate = Date().createDateFromFormat(format, expire)
                        if (strings.count() > 0)
                            entry.reason = strings.pop().trim()
                    }
                }
            }
            return entry
        }
    }

    var name: String = ""

    var creationDate: Date? = null

    var source = "(Unknown)"

    var expirationDate: Date? = null

    var reason = "Banned by an operator."

    init {
        this.name = name.toLowerCase()
        this.creationDate = Date()
    }

    fun hasExpired(): Boolean {
        val now = Date()
        return if (expirationDate == null) false else (expirationDate!!.after(now))
    }

    fun getString(): String {
        val str = StringBuilder()
        str.append(name)
        str.append("|")
        str.append(format.format(creationDate!!))
        str.append("|")
        str.append(source)
        str.append("|")
        str.append(if (expirationDate == null) "Forever" else format.format(expirationDate!!))
        str.append("|")
        str.append(reason)

        return str.toString()
    }
}