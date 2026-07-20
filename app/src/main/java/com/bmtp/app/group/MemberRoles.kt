package com.bmtp.app.group

/**
 * Defines hierarchical roles within a decentralized group.
 * Lower ordinal value implies higher privilege (Owner = 0).
 */
enum class MemberRole(val level: Int) {
    OWNER(0),
    ADMINISTRATOR(1),
    MODERATOR(2),
    MEMBER(3),
    GUEST(4),
    READ_ONLY(5);

    fun canManage(other: MemberRole): Boolean {
        return this.level < other.level
    }
}
