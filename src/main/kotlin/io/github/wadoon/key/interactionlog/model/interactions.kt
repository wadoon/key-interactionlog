/* This file is part of key-abbrevmgr.
 * key-abbrevmgr is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
@file:Suppress("unused")

package io.github.wadoon.key.interactionlog.model

import de.uka.ilkd.key.control.InteractionListener
import de.uka.ilkd.key.gui.MainWindow
import de.uka.ilkd.key.macros.ProofMacro
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo
import de.uka.ilkd.key.nparser.ParsingFacade
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.rule.TacletApp
import de.uka.ilkd.key.scripts.ScriptException
import de.uka.ilkd.key.settings.Configuration
import de.uka.ilkd.key.ui.AbstractMediatorUserInterfaceControl
import io.github.wadoon.key.interactionlog.algo.LogPrinter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.antlr.v4.runtime.CharStreams
import org.key_project.logic.PosInTerm
import org.key_project.prover.engine.ProofSearchInformation
import org.key_project.prover.rules.RuleApp
import org.key_project.prover.sequent.PosInOccurrence
import org.key_project.prover.sequent.Sequent
import org.key_project.util.RandomName
import java.awt.Color
import java.io.File
import java.io.StringWriter
import java.lang.Character.isWhitespace
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.JOptionPane
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/**
 * @author Alexander Weigl
 * @version 1 (06.12.18)
 */
@Serializable
data class InteractionLog(val name: String = RandomName.getRandomName(), var created: LocalDateTime = now()) {
    @Transient
    var proof: WeakReference<Proof> = WeakReference<Proof>(null)

    @Transient
    val listeners = arrayListOf<() -> Unit>()

    @Transient
    var savePath: File? = null

    private val _interactions: MutableList<Interaction> = ArrayList()

    val interactions: List<Interaction>
        get() = _interactions


    fun add(interaction: Interaction) = _interactions.add(interaction)
    fun remove(interaction: Interaction) = _interactions.remove(interaction)
    fun remove(index: Int) = _interactions.removeAt(index)

    companion object {
        fun fromProof(proof: Proof): InteractionLog {
            val pos = proof.name().toString().length.coerceAtMost(25)
            val name = "${RandomName.getRandomName(" ")} (${proof.name().toString().substring(0, pos)})"
            val a = InteractionLog(name)
            a.proof = WeakReference(proof)
            return a
        }
    }

    override fun toString(): String = name
}

@Serializable
sealed class NodeInteraction() : Interaction() {
    var nodeIdentifier: NodeIdentifier? = null

    @Transient
    var serialNr: Int? = null

    constructor(node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        serialNr = node.serialNr()
    }

    fun getNode(proof: Proof): Node? = nodeIdentifier?.findNode(proof)
}


/**
 * @author Alexander Weigl
 */
@Serializable
class MacroInteraction() : NodeInteraction() {
    var macroName: String? = null
    var macro: ProofMacro? = null

    @Contextual
    var pos: PosInOccurrence? = null
    var info: String? = null
    var openGoalSerialNumbers: List<Int>? = null
    var openGoalNodeIds: List<NodeIdentifier>? = null

    override val markdown: String
        get() = """
        ## Applied macro $macro

        ```
        $info
        ```
        """.trimIndent()

    override val proofScriptRepresentation: String
        get() = "macro $macro;\n"

    constructor(node: Node, macro: ProofMacro, posInOcc: PosInOccurrence?, info: ProofMacroFinishedInfo) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        serialNr = node.serialNr()

        this.info = info.toString()
        macroName = macro.scriptCommandName
        pos = posInOcc
        val openGoals = info.proof.openGoals()
        this.openGoalSerialNumbers = openGoals.toList().map { g -> g.node().serialNr() }
        this.openGoalNodeIds = openGoals.toList().map { g -> NodeIdentifier.create(g.node()) }
    }

    override fun toString(): String = macroName ?: "n/a"

    val macros by lazy {
        ServiceLoader.load(ProofMacro::class.java).toList()
    }

    @Throws(Exception::class)
    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        val macro = macros.find { it.name == macroName }
        val pio = pos
        if (macro != null) {
            if (!macro.canApplyTo(goal.node(), pio)) {
                throw IllegalStateException("Macro not applicable")
            }

            try {
                macro.applyTo(uic, goal.node(), pio, uic)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (06.12.18)
 */
@Serializable
data class NodeIdentifier(
    var treePath: MutableList<Int> = ArrayList(),
    var branchLabel: String? = null,
    var serialNr: Int = 0
) {

    constructor(vararg seq: Int) : this(seq.toList())

    constructor(seq: List<Int>) : this() {
        this.treePath.addAll(seq)
    }

    override fun toString() =
        treePath.stream()
            .map { it.toString() }
            .reduce("") { a, b -> a + b } +
                " => " + serialNr


    fun findNode(proof: Proof) = findNode(proof.root())

    fun findNode(node: Node): Node? {
        var n = node
        for (child in treePath) {
            if (child <= n.childrenCount()) {
                n = n.child(child)
            } else {
                return null
            }
        }
        return n
    }

    companion object {
        fun create(g: Goal): NodeIdentifier = create(g.node())

        fun create(node: Node): NodeIdentifier {
            var n: Node? = node
            val list = LinkedList<Int>()
            do {
                val parent = n?.parent()
                if (parent != null) {
                    list.add(0, parent.getChildNr(n))
                }
                n = parent
            } while (n != null)
            val ni = NodeIdentifier(list)
            ni.branchLabel = LogPrinter.getBranchingLabel(n)
            return ni
        }
    }
}

@Serializable
class PruneInteraction() : NodeInteraction() {
    override val markdown: String
        get() = """
            ## Prune

            * **Date**: $created
            * Prune to node: `$nodeIdentifier`
            """.trimIndent()

    override val proofScriptRepresentation: String
        get() = "prune $nodeIdentifier\n"

    override fun toString(): String = "prune"

    constructor(node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        serialNr = node.serialNr()
    }


    @Throws(Exception::class)
    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        nodeIdentifier?.findNode(goal.proof())?.also {
            goal.proof().pruneProof(it)
        }
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
@Serializable
class OccurenceIdentifier {
    var path: Array<Int>? = null
    var term: String? = null
    var termHash: Int = 0
    var toplevelFormula: String? = null
    var formulaNumber: Int = 0
    var isAntec: Boolean = false

    override fun toString(): String {
        return path?.let {
            if (it.isNotEmpty()) {
                "$term under $toplevelFormula(Path: ${path.contentToString()})"
            } else {
                "$term @toplevel"
            }
        } ?: " @toplevel"
    }

    fun rebuildOn(goal: Goal) = rebuildOn(goal.node().sequent())

    private fun rebuildOn(seq: Sequent): PosInOccurrence {
        val path = path
        val pit = if (path != null && path.isNotEmpty())
            PosInTerm(path.toIntArray())
        else
            PosInTerm.getTopLevel()

        return PosInOccurrence.findInSequent(seq, formulaNumber, pit)
    }

    companion object {
        fun create(seq: Sequent, p: PosInOccurrence?): OccurenceIdentifier {
            if (p == null) return OccurenceIdentifier()

            val indices = ArrayList<Int>()
            val iter = p.iterator()
            while (iter.next() != -1) {
                indices.add(iter.child)
            }

            val occ = OccurenceIdentifier()
            occ.formulaNumber = seq.formulaNumberInSequent(p.isInAntec, p.sequentFormula())
            occ.path = indices.toTypedArray()
            occ.term = iter.subTerm.toString()
            occ.termHash = iter.subTerm.hashCode()
            occ.toplevelFormula = p.topLevel().subTerm().toString()
            occ.isAntec = p.isInAntec
            return occ
        }
    }
}


@Serializable
class UserNoteInteraction(var note: String = "") : Interaction() {

    override val markdown: String
        get() = """
                ## Note

                **Date**: $created

                > ${note.replace("\n", "\n> ")}
                """.trimIndent()

    init {
        graphicalStyle.backgroundColor = Color.red.brighter().brighter().brighter()
    }

    override fun toString(): String = note
}


@Serializable
class SettingChangeInteraction() : Interaction() {
    var savedSettings: String? = null
    var type: InteractionListener.SettingType? = null
    var message: String? = null

    override val markdown: String
        get() {
            return """
            # Setting changed: ${type?.name}

            **Date**: $created

            """.trimIndent() + """
                |```
                |${savedSettings}
                |```
                |
            """.trimMargin()
        }

    constructor(settings: Configuration, type: InteractionListener.SettingType) : this() {
        graphicalStyle.backgroundColor = Color.WHITE
        graphicalStyle.foregroundColor = Color.gray
        this.type = type
        this.savedSettings = StringWriter().use {
            settings.save(it, "")
            it.toString()
        }
    }

    override fun toString(): String = (if (message != null) message!! + " : " else "") + type

    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        val settings = goal.proof().settings
        val p = savedSettings?.let {
            ParsingFacade.parseConfigurationFile(CharStreams.fromString(savedSettings))
                .asConfiguration()
        } ?: error("No settings stored")

        when (type) {
            InteractionListener.SettingType.SMT -> settings.smtSettings.writeSettings(p)
            InteractionListener.SettingType.CHOICE -> settings.choiceSettings.writeSettings(p)
            InteractionListener.SettingType.STRATEGY -> settings.strategySettings.writeSettings(p)
            null -> TODO()
        }
    }
}


private fun Properties.toStringMap(): Map<String, String> =
    asSequence().map { (k, v) -> k.toString() to v.toString() }.toMap()


@Serializable
class AutoModeInteraction(
    var infoMessage: String? = null,
    var timeInMillis: Long = 0,
    var appliedRuleAppsCount: Int = 0,
    var errorMessage: String? = null,
    var nrClosedGoals: Int = 0
) : Interaction() {
    var initialNodeIds: List<NodeIdentifier> = arrayListOf()
    var openGoalNodeIds: List<NodeIdentifier> = arrayListOf()

    override val markdown: String
        get() {
            val initialNodes = initialNodeIds.joinToString("\n") { nr -> "  * `$nr`" }
            val finalNodes = openGoalNodeIds.joinToString("\n") { nr -> "  * `$nr`" }

            return """
            ## Apply auto strategy

            **Date**: $created

            * Started on node:
            $initialNodes

            ${
                if (openGoalNodeIds.isEmpty()) "* **Closed all goals**"
                else "* Finished on nodes:"
            }}
            $finalNodes

            * Provided Macro info:
              * Info message: $infoMessage
              * Time $timeInMillis ms
              * Applied rules: $appliedRuleAppsCount
              * Error message: $errorMessage
              * Closed goals $nrClosedGoals
            """.trimIndent()
        }

    override val proofScriptRepresentation: String
        get() = "auto;%n"

    constructor(initialNodes: List<Node>, info: ProofSearchInformation<Proof, Goal>) : this(
        info.reason(),
        info.time,
        info.numberOfAppliedRuleApps,
        info.exception?.message,
        info.numberOfClosedGoals
    ) {
        this.initialNodeIds = initialNodes.map { NodeIdentifier.create(it) }
        val openGoals = info.proof.openGoals()
        this.openGoalNodeIds = openGoals.toList().map { NodeIdentifier.create(it) }
    }

    override fun toString(): String = "Auto Mode"

    @Throws(Exception::class)
    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        uic.proofControl.startAutoMode(goal.proof(), goal.proof().openGoals(), uic)
    }
}


/**
 * @author weigl
 */
@Serializable
class RuleInteraction() : NodeInteraction() {
    var ruleName: String? = null
    var posInOccurence: OccurenceIdentifier? = null
    var arguments = HashMap<String, String>()
    var ruleOccurence: Int? = null

    constructor(node: Node, app: RuleApp) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        serialNr = node.serialNr()

        ruleName = app.rule().displayName()
        this.posInOccurence = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        if (app is TacletApp) {
            arguments = HashMap(app.arguments())
            /*SequentFormula seqForm = pos.getPosInOccurrence().sequentFormula();
                String sfTerm = LogicPrinter.quickPrintTerm(seqForm.formula(), services);
                String onTerm = LogicPrinter.quickPrintTerm(pos.getPosInOccurrence().subTerm(), services);
                sb.append("\n    formula=`").append(sfTerm).append("`");
                sb.append("\n    on=`").append(onTerm).append("`");
                sb.append("\n    occ=?;");
                */

        }
    }

    override val markdown: String
        get() {
            val formula = posInOccurence
            val parameters =
                arguments.map { (key, value) -> "              * $key : `$value`" }
                    .joinToString("\n")

            return """
            ## Rule `$ruleName` applied

            **Date**: $created

            * Applied on `$formula`
            * The used parameter for the taclet instantation are
            """.trimIndent() +
                    if (arguments.isEmpty()) "empty" else parameters
        }

    override val proofScriptRepresentation: String
        get() {
            val args =
                if (arguments.isEmpty()) ""
                else
                    arguments.map { (k, v) ->
                        "                 inst_${firstWord(k)} = \"${v.trim { it <= ' ' }}\"\n"
                    }.joinToString("\n")

            return """
            rule $ruleName
                 on = "${posInOccurence?.term}"
                 formula = "${posInOccurence?.toplevelFormula}"
                 $args;
            """.trimIndent()
        }

    override fun toString(): String = ruleName ?: "n/a"

    private fun firstWord(k: String): String {
        val t = k.trim { it <= ' ' }
        return t.takeWhile { isWhitespace(it) }
    }

    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        val rh = RuleHelper(goal, ruleName!!, posInOccurence, arguments, ruleOccurence, true)
        try {
            var theApp = rh.makeRuleApp()
            if (theApp is TacletApp) {
                val completeApp: RuleApp? = theApp.tryToInstantiate(goal.proof().services)
                theApp = completeApp ?: theApp
            }
            theApp?.let { goal.apply(it) }
        } catch (e: ScriptException) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), e.message)
        }
    }
}
