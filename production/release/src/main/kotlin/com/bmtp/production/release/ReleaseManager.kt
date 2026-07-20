package com.bmtp.production.release

import java.time.LocalDate

/**
 * Automates the validation of SemVer rules and generation of Release Notes.
 */
object ReleaseManager {

    fun generateReleaseNotes(version: String, isBreaking: Boolean, changes: List<String>): String {
        require(version.matches(Regex("^v\\d+\\.\\d+\\.\\d+$"))) { "Invalid SemVer format." }

        val type = if (isBreaking) "MAJOR RELEASE" else "MINOR/PATCH RELEASE"
        
        val notes = StringBuilder()
        notes.appendLine("# Antigravity Protocol Release $version")
        notes.appendLine("**Date**: ${LocalDate.now()}")
        notes.appendLine("**Type**: $type")
        notes.appendLine()
        
        if (isBreaking) {
            notes.appendLine("> [!WARNING]")
            notes.appendLine("> **Breaking Changes Included**")
            notes.appendLine("> This release modifies the core packet structure. Older nodes will not be able to parse these packets.")
            notes.appendLine()
        }

        notes.appendLine("## Changelog")
        changes.forEach { notes.appendLine("- $it") }

        return notes.toString()
    }
}
