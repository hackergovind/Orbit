package com.bmtp.production.observability

/**
 * A logger designed for machine-readability (JSON output) in production environments
 * like ELK, Datadog, or Splunk.
 *
 * It strictly forbids logging sensitive fields (like payload bytes).
 */
object StructuredLogger {
    
    fun logEvent(event: String, tags: Map<String, String>) {
        val jsonParts = mutableListOf<String>()
        jsonParts.add("\"event\": \"$event\"")
        jsonParts.add("\"timestamp\": ${System.currentTimeMillis()}")
        
        tags.forEach { (key, value) ->
            // Prevent raw payload logging
            if (key.contains("payload", ignoreCase = true) || key.contains("key", ignoreCase = true)) {
                jsonParts.add("\"$key\": \"[REDACTED]\"")
            } else {
                jsonParts.add("\"$key\": \"$value\"")
            }
        }
        
        println("{ ${jsonParts.joinToString(", ")} }")
    }
}
