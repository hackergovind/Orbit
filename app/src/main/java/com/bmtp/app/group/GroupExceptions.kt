package com.bmtp.app.group

open class GroupException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GroupNotFoundException(message: String) : GroupException(message)

class AlreadyMemberException(message: String) : GroupException(message)

class PermissionDeniedException(message: String) : GroupException(message)

class InvalidInvitationException(message: String) : GroupException(message)

class RoleConflictException(message: String) : GroupException(message)

class GroupEncryptionException(message: String, cause: Throwable? = null) : GroupException(message, cause)

class SynchronizationException(message: String, cause: Throwable? = null) : GroupException(message, cause)

class MembershipVerificationException(message: String) : GroupException(message)

class ChannelNotFoundException(message: String) : GroupException(message)
