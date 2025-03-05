package io.github.wadoon.key.interactionlog

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import io.github.wadoon.key.interactionlog.model.InteractionLog
import java.io.File


/**
 * @author Alexander Weigl
 * @version 1 (06.12.18)
 */
@OptIn(ExperimentalSerializationApi::class)
object InteractionLogFacade {
    internal val config = Json {
        prettyPrint = true
        prettyPrintIndent = "    "
        ignoreUnknownKeys = true
    }

    /**
     * @param inputFile
     * @return
     */
    @JvmStatic
    fun readInteractionLog(inputFile: File): InteractionLog =
        inputFile.inputStream().use {
            config.decodeFromStream<InteractionLog>(it).also { log ->
                log.savePath = inputFile
            }
        }

    /**
     * @param log
     * @param output
     */
    @JvmStatic
    fun storeInteractionLog(log: InteractionLog, output: File) {
        val out = config.encodeToString(log)
        output.bufferedWriter().use {
            it.write(out)
        }
    }
}
