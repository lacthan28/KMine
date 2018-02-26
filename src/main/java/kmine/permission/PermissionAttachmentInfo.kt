package kmine.permission

class PermissionAttachmentInfo(private var permissible: Permissible, private var permission: String?, private var attachment: PermissionAttachment, private var value: Boolean) {

    fun getPermissible(): Permissible = permissible

    fun getPermission(): String = permission!!

    fun getAttachment(): PermissionAttachment = attachment

    fun getValue(): Boolean = value

    init {
        if (permission == null) {
            throw IllegalStateException("Permission may not be null")
        }
    }
}