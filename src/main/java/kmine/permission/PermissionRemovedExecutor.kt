package kmine.permission

interface PermissionRemovedExecutor {
    fun attachmentRemoved(attachment: PermissionAttachment)
}