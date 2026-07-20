package com.bmtp.app.voice

open class VoiceException(message: String, cause: Throwable? = null) : Exception(message, cause)

class VoiceSessionException(message: String, cause: Throwable? = null) : VoiceException(message, cause)

class CodecFailureException(message: String, cause: Throwable? = null) : VoiceException(message, cause)

class AudioCaptureException(message: String, cause: Throwable? = null) : VoiceException(message, cause)

class AudioPlaybackException(message: String, cause: Throwable? = null) : VoiceException(message, cause)

class PacketLossException(message: String) : VoiceException(message)

class BufferOverflowException(message: String) : VoiceException(message)

class VoiceTimeoutException(message: String) : VoiceException(message)

class VoiceEncryptionException(message: String, cause: Throwable? = null) : VoiceException(message, cause)
