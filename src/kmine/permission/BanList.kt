package kmine.permission

class BanList(var file: String) {
    var list: Map<String, BanEntry> = emptyMap()
        get() {
            removeExpire()
            return field
        }

    var isEnabled = true

    fun getEntry(name: String): BanEntry? {
        removeExpire()
        return list[name.toLowerCase()]
    }

    fun isBanned(name: String): Boolean {
        if (!isEnabled) return false
        else
            removeExpire()
        return list[name.toLowerCase()] != null
    }

    fun add(entry: BanEntry) {
        list.getOrElse(entry.name, defaultValue = {
            this.add(entry)
        })
        save()
    }
}