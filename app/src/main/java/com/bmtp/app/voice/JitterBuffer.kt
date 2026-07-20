package com.bmtp.app.voice

import java.util.PriorityQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles out-of-order delivery, variable network latency (jitter), and packet loss.
 * Feeds frames to the AudioPlayer at a consistent rate.
 */
@Singleton
class JitterBuffer @Inject constructor(
    private val config: VoiceConfig,
    private val stats: VoiceStatistics,
    private val logger: VoiceLogger
) {
    // Queue sorted by Sequence Number for ordered playback
    private val buffer = PriorityQueue<EncodedVoiceFrame>(10) { f1, f2 ->
        f1.sequenceNumber.compareTo(f2.sequenceNumber)
    }
    
    private val lock = ReentrantLock()
    
    private var nextExpectedSeq = -1
    private var isBuffering = true
    
    private val maxFrames = config.maxJitterBufferMs / config.frameDurationMs
    private val initialFrames = config.initialJitterBufferMs / config.frameDurationMs

    /**
     * Pushes a received frame into the buffer.
     * Drops it if it's too late or if the buffer is overflowing.
     */
    fun push(frame: EncodedVoiceFrame) {
        lock.withLock {
            if (nextExpectedSeq != -1 && frame.sequenceNumber < nextExpectedSeq) {
                // Packet arrived too late, we already played past it
                stats.recordPacketDropped()
                return
            }

            if (buffer.size >= maxFrames) {
                // Buffer Overflow: Network is severely delayed or playback thread stalled
                val dropped = buffer.poll()
                stats.recordJitterOverflow()
                stats.recordPacketDropped()
                if (dropped != null) {
                    logger.logJitterBufferOverflow(dropped.sequenceNumber)
                }
            }

            buffer.offer(frame)

            // Once we have enough frames, exit buffering state
            if (isBuffering && buffer.size >= initialFrames) {
                isBuffering = false
            }
        }
    }

    /**
     * Pops the next frame for playback. 
     * Returns null if buffering, or if a packet was lost (triggering PLC in the decoder).
     */
    fun pop(): EncodedVoiceFrame? {
        lock.withLock {
            if (isBuffering) {
                return null // Tell player to wait
            }

            if (buffer.isEmpty()) {
                // Underrun: We ran out of packets!
                isBuffering = true
                nextExpectedSeq = -1
                stats.recordJitterUnderrun()
                logger.logJitterBufferUnderrun()
                return null
            }

            val nextFrame = buffer.peek()

            if (nextExpectedSeq == -1) {
                // First frame after buffering
                nextExpectedSeq = nextFrame.sequenceNumber
            }

            if (nextFrame.sequenceNumber > nextExpectedSeq) {
                // Packet Loss: The frame we want isn't here yet.
                // We return null to trigger Packet Loss Concealment (PLC) in the decoder.
                // We advance expected seq but leave the future frame in the buffer.
                nextExpectedSeq++
                stats.recordPacketDropped() // Technically lost, not dropped locally
                
                // Track loss for adaptive bitrate
                val latencyMs = calculateCurrentLatencyMs()
                stats.updateNetworkQuality(latencyMs, 0.1f) // Fake 10% loss spike on missing seq
                
                return null 
            }

            // Normal case: We got the frame we expected
            buffer.poll()
            nextExpectedSeq++
            
            // Periodically update latency stats
            stats.updateNetworkQuality(calculateCurrentLatencyMs(), 0.0f)
            
            return nextFrame
        }
    }
    
    private fun calculateCurrentLatencyMs(): Float {
        // Rough estimate based on buffer depth
        return (buffer.size * config.frameDurationMs).toFloat()
    }

    fun flush() {
        lock.withLock {
            buffer.clear()
            nextExpectedSeq = -1
            isBuffering = true
        }
    }
}
