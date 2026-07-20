package com.bmtp.app.voice

/**
 * Configuration parameters for the BMTP Real-Time Voice Communication Layer.
 */
data class VoiceConfig(
    /** Desired sample rate for audio capture (e.g., 16000 or 48000 Hz). 16kHz is wideband, good for speech. */
    val sampleRate: Int = 16000,
    
    /** Frame size in milliseconds (e.g., 20ms is standard for Opus). */
    val frameDurationMs: Int = 20,
    
    /** Base bitrate for the encoder in bits per second (bps). E.g., 16,000 bps. */
    val baseBitrateBps: Int = 16000,
    
    /** Minimum allowed adaptive bitrate in bps (e.g., 8,000 bps). */
    val minBitrateBps: Int = 8000,
    
    /** Maximum allowed adaptive bitrate in bps (e.g., 32,000 bps). */
    val maxBitrateBps: Int = 32000,
    
    /** Initial capacity of the Jitter Buffer in milliseconds. */
    val initialJitterBufferMs: Int = 100,
    
    /** Maximum capacity of the Jitter Buffer in milliseconds before forcing drops. */
    val maxJitterBufferMs: Int = 500,
    
    /** Number of hops (TTL) for broadcast voice packets. */
    val voiceBroadcastTtl: Int = 5,
    
    /** 
     * If true, uses unreliable delivery (MeshForwarding) for live voice.
     * If false, falls back to ReliableTransport (not recommended for live audio due to head-of-line blocking).
     */
    val useUnreliableTransportForLive: Boolean = true
) {
    /** Number of samples per frame. */
    val samplesPerFrame: Int = sampleRate * frameDurationMs / 1000
    
    /** Size of the raw PCM frame in bytes (assuming 16-bit Mono). */
    val pcmFrameSizeBytes: Int = samplesPerFrame * 2
}
