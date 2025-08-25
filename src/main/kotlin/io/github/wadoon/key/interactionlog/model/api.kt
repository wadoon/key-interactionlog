/* This file is part of key-abbrevmgr.
 * key-abbrevmgr is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
package io.github.wadoon.key.interactionlog.model

import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.ui.AbstractMediatorUserInterfaceControl
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import io.github.wadoon.key.interactionlog.InteractionRecorder
import java.awt.Color
import javax.swing.Icon

/**
 * @author Alexander Weigl
 */

@Serializable
sealed class Interaction : Markdownable, Scriptable, Reapplicable {
    @Transient
    var graphicalStyle = InteractionGraphicStyle()
        protected set

    var created: LocalDateTime = now()

    var isFavoured = false
}

data class InteractionGraphicStyle(
    var icon: Icon? = null, var backgroundColor: Color? = null,
    var foregroundColor: Color? = null
)

/**
 * An interaction recoder listener receives interactions to store them.
 */
interface InteractionRecorderListener {
    fun onInteraction(recorder: InteractionRecorder, log: InteractionLog, event: Interaction) {}
    fun onNewInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {}
    fun onDisposeInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {}
}

/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Scriptable {
    @Transient
    val proofScriptRepresentation: String
        get() = "// Unsupported interaction: $javaClass\n"
}

/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Reapplicable {
    fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        throw UnsupportedOperationException()
    }

    fun reapplyRelaxed(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        throw UnsupportedOperationException()
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Markdownable {
    @Transient
    val markdown: String
        get() = String.format("**Unsupported interaction: %s**%n%n", this.javaClass.name)
}
