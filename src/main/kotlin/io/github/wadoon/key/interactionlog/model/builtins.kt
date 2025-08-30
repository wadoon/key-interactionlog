/* This file is part of key-interactionlog.
 * key-interactionlog is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
@file:Suppress("unused")

package io.github.wadoon.key.interactionlog.model

import de.uka.ilkd.key.gui.WindowUserInterfaceControl
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.rule.*
import de.uka.ilkd.key.rule.merge.MergeRuleBuiltInRuleApp
import de.uka.ilkd.key.smt.SMTRuleApp
import kotlinx.serialization.Serializable
import org.key_project.prover.sequent.PosInOccurrence
import org.slf4j.LoggerFactory

object BuiltInRuleInteractionFactory {
    private val LOGGER = LoggerFactory.getLogger(BuiltInRuleInteractionFactory.javaClass)
    fun <T : IBuiltInRuleApp> create(node: Node, app: T): BuiltInRuleInteraction? = when (app) {
        is OneStepSimplifierRuleApp -> OSSBuiltInRuleInteraction(app, node)
        is ContractRuleApp -> ContractBuiltInRuleInteraction(app, node)
        is UseDependencyContractApp -> UseDependencyContractBuiltInRuleInteraction(app, node)
        is LoopContractInternalBuiltInRuleApp -> LoopContractInternalBuiltInRuleInteraction(app, node)
        is LoopInvariantBuiltInRuleApp -> LoopInvariantBuiltInRuleInteraction(app, node)
        is MergeRuleBuiltInRuleApp -> MergeRuleBuiltInRuleInteraction(app, node)
        is SMTRuleApp -> SMTBuiltInRuleInteraction(app, node)
        else -> {
            LOGGER.warn("Unknown rule app {}", app::class.qualifiedName)
            null
        }
    }
}

@Serializable
sealed class BuiltInRuleInteraction() : NodeInteraction() {
    var ruleName: String? = null
    var occurenceIdentifier: OccurenceIdentifier? = null

    constructor(node: Node, pio: PosInOccurrence) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        serialNr = node.serialNr()

        this.occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), pio)
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class ContractBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var contractType: String? = null
    var contractName: String? = null

    constructor(app: ContractRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        contractName = app.instantiation.name
        contractType = app.instantiation.typeName
    }

    override fun toString() = "Contract $contractName applied"

    override val proofScriptRepresentation: String
        get() = "contract $contractName"
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class LoopContractInternalBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var displayName: String? = null
    var contractName: String? = null

    constructor(app: LoopContractInternalBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        contractName = app.contract.name
        displayName = app.contract.displayName
        println(app.statement)
        println(app.executionContext)
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class LoopInvariantBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var displayName: String? = null
    var contractName: String? = null

    constructor(app: LoopInvariantBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        println(app.loopStatement)
        println(app.executionContext)
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class MergeRuleBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: MergeRuleBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class OSSBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    override val markdown: String
        get() = String.format(
            "## One step simplification%n" + "* applied on %n  * Term:%s%n  * Toplevel %s%n",
            occurenceIdentifier?.term,
            occurenceIdentifier?.toplevelFormula,
        )

    override val proofScriptRepresentation: String
        get() = String.format(
            "one_step_simplify %n" +
                "\t     on = \"%s\"%n" +
                "\tformula = \"%s\"%n;%n",
            occurenceIdentifier?.term,
            occurenceIdentifier?.toplevelFormula,
        )

    constructor(app: OneStepSimplifierRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }

    override fun toString(): String = "one step simplification on" + occurenceIdentifier?.term

    fun reapply(uic: WindowUserInterfaceControl, goal: Goal) {
        val oss = OneStepSimplifier()
        val pio = occurenceIdentifier!!.rebuildOn(goal)
        val app = oss.createApp(pio, goal.proof().services)
        goal.apply(app)
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class SMTBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: SMTRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        println(app.assumesInsts())
    }

    override fun toString(): String = "SMT built-in rule"

    override val proofScriptRepresentation: String
        get() = "smt"
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class UseDependencyContractBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: UseDependencyContractApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }
}
