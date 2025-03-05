package io.github.wadoon.key.interactionlog.algo

import io.github.wadoon.key.interactionlog.model.InteractionLog

import java.io.PrintWriter

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class KPSProofScriptExport(logbook: InteractionLog, writer: PrintWriter) : MUProofScriptExport(logbook, writer) {
    companion object {
        fun writeTo(logbook: InteractionLog, writer: PrintWriter) {
            writer.format("// Proof script: *%s*%n", logbook.name)
            writer.format("// Created at *%s*%n", logbook.created)
        }
    }
}