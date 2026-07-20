package com.bmtp.production.fuzzing

import kotlin.random.Random

/**
 * Generates malformed and mathematically impossible packets to test the resilience
 * of the protocol parser and security engine.
 */
object FuzzGenerator {

    /**
     * Generates a packet with completely random garbage bytes.
     */
    fun generateGarbagePacket(size: Int): ByteArray {
        return Random.nextBytes(size)
    }

    /**
     * Generates a packet that has the correct Magic Bytes ("AG") but invalid lengths and headers.
     */
    fun generateDeceptivePacket(size: Int): ByteArray {
        val packet = Random.nextBytes(size)
        if (packet.size >= 2) {
            packet[0] = 0x41 // 'A'
            packet[1] = 0x47 // 'G'
        }
        return packet
    }

    /**
     * Mutates a valid packet by flipping random bits.
     */
    fun mutateValidPacket(validPacket: ByteArray, bitFlipProbability: Double = 0.01): ByteArray {
        val mutated = validPacket.copyOf()
        for (i in mutated.indices) {
            if (Random.nextDouble() < bitFlipProbability) {
                // Flip a random bit in this byte
                val bitIndex = Random.nextInt(8)
                mutated[i] = (mutated[i].toInt() xor (1 shl bitIndex)).toByte()
            }
        }
        return mutated
    }
}
