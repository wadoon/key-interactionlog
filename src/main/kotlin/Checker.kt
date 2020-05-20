package de.uka.ilkd.key

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import de.uka.ilkd.key.api.KeYApi
import de.uka.ilkd.key.api.ProofManagementApi
import de.uka.ilkd.key.control.AbstractProofControl
import de.uka.ilkd.key.control.AbstractUserInterfaceControl
import de.uka.ilkd.key.control.KeYEnvironment
import de.uka.ilkd.key.control.UserInterfaceControl
import de.uka.ilkd.key.logic.PosInOccurrence
import de.uka.ilkd.key.macros.*
import de.uka.ilkd.key.macros.scripts.ProofScriptEngine
import de.uka.ilkd.key.parser.Location
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.prover.ProverTaskListener
import de.uka.ilkd.key.settings.ChoiceSettings
import de.uka.ilkd.key.settings.ProofSettings
import de.uka.ilkd.key.speclang.Contract
import de.uka.ilkd.key.util.KeYConstants
import de.uka.ilkd.key.util.MiscTools
import org.key_project.util.collection.ImmutableList
import java.io.File
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


const val ESC = 27.toChar()
const val RED = 31;
const val GREEN = 32;
const val YELLOW = 33;
const val BLUE = 34;
const val MAGENTA = 35;
const val CYAN = 36;
fun color(s: Any, c: Int) = "${ESC}[${c}m$s${ESC}[0m"

/**
 * A small interface for a checker scripts
 * @author Alexander Weigl
 * @version 1 (21.11.19)
 */
class Checker : CliktCommand() {
    val junitXmlOutput by option("--xml-output").file()

    val enableMeasuring: Boolean by option("--measuring",
            help = "try to measure proof coverage").flag()

    val includes by option(
            help = "defines additional key files to be included"
    ).multiple()
    val autoModeStep by option("--auto-mode-max-step", metavar = "INT",
            help = "maximal amount of steps in auto-mode [default:10000]")
            .int().default(10000)
    val verbose by option("-v", "--verbose", help = "verbose output, currently unused")
            .flag("--no-verbose")
    val dryRun by option("--dry-run",
            help = "skipping the proof reloading, scripts execution and auto mode." +
                    " Useful for finding the contract names").flag()

    val classpath by option("--classpath", "-cp",
            help = "additional classpaths").multiple()

    val bootClassPath by option("--bootClassPath", "-bcp",
            help = "set the bootclasspath")

    val onlyContracts by option("--contract",
            help = "whitelist contracts by their names")
            .multiple()

    val forbidContracts by option("--forbid-contact",
            help = "forbid contracts by their name")
            .multiple()

    val inputFile by argument("JAVA-KEY-FILE",
            help = "key, java or a folder")
            .multiple(true)

    val proofPath by option("--proof-path",
            help = "folders to look for proofs and script files")
            .multiple()

    private var choiceSettings: ChoiceSettings? = null

    private fun initEnvironment() {
        if (!ProofSettings.isChoiceSettingInitialised()) {
            val env: KeYEnvironment<*> = KeYEnvironment.load(File("."), null, null, null)
            env.dispose()
        }
        choiceSettings = ProofSettings.DEFAULT_SETTINGS.choiceSettings
    }

    var errors = 0

    var testSuites = TestSuites()

    override fun run() {
        printm("KeY version: ${KeYConstants.VERSION}")
        printm("KeY internal: ${KeYConstants.INTERNAL_VERSION}")
        printm("Copyright: ${KeYConstants.COPYRIGHT}")
        printm("More information at: https://formal.iti.kit.edu/weigl/ci-tool/")

        testSuites.name = inputFile.joinToString(" ")

        inputFile.forEach { run(it) }

        junitXmlOutput?.let { file ->
            file.bufferedWriter().use {
                testSuites.writeXml(it)
            }
        }

        exitProcess(errors)
    }

    var currentPrintLevel = 0;
    fun printBlock(message: String, f: () -> Unit) {
        printm(message)
        currentPrintLevel++
        f()
        currentPrintLevel--
    }


    fun printm(message: String, fg: Int? = null) {
        print("  ".repeat(currentPrintLevel))
        if (fg != null)
            println(color(message, fg))
        else
            println(message)
    }

    fun run(inputFile: String) {
        printBlock("[INFO] Start with `$inputFile`") {
            val pm = KeYApi.loadProof(File(inputFile),
                    classpath.map { File(it) },
                    bootClassPath?.let { File(it) },
                    includes.map { File(it) })

            val contracts = pm.proofContracts
                    .filter { it.name in onlyContracts || onlyContracts.isEmpty() }

            printm("[INFO] Found: ${contracts.size}")
            var successful = 0
            var ignored = 0
            var failure = 0

            val testSuite = testSuites.newTestSuite(inputFile)
            ProofSettings.DEFAULT_SETTINGS.properties.forEach { t, u ->
                testSuite.properties[t.toString()] = u
            }

            for (c in contracts) {
                val testCase = testSuite.newTestCase(c.name)
                printBlock("[INFO] Contract: `${c.name}`") {
                    val filename = MiscTools.toValidFileName(c.name)
                    when {
                        c.name in forbidContracts -> {
                            printm(" [INFO] Contract excluded by `--forbid-contract`")
                            testCase.result = TestCaseKind.Skipped("Contract excluded by `--forbid-contract`.")
                            ignored++
                        }
                        dryRun -> {
                            printm("[INFO] Contract skipped by `--dry-run`")
                            testCase.result = TestCaseKind.Skipped("Contract skipped by `--dry-run`.")
                            ignored++
                        }
                        else -> {
                            val b = runContract(pm, c, filename)
                            if (b) {
                                //testCase.result = TestCaseKind.Skipped("Contract excluded by `--forbid-contract`.")
                                successful++
                            } else {
                                testCase.result = TestCaseKind.Failure("Proof not closeable.")
                                failure++
                            }
                        }
                    }
                }
            }
            printm("[INFO] Summary for $inputFile: " +
                    "(successful/ignored/failure) " +
                    "(${color(successful, GREEN)}/${color(ignored, BLUE)}/${color(failure, RED)})")
            if (failure != 0)
                printm("[ERR ] $inputFile failed!", fg = RED)
        }
    }

    private fun runContract(pm: ProofManagementApi, c: Contract?, filename: String): Boolean {
        val proofApi = pm.startProof(c)
        val proof = proofApi.proof
        require(proof != null)
        proof.settings?.strategySettings?.maxSteps = autoModeStep
        ProofSettings.DEFAULT_SETTINGS.strategySettings.maxSteps = autoModeStep

        val proofFile = findProofFile(filename)
        val scriptFile = findScriptFile(filename)
        val ui = proofApi.env.ui as AbstractUserInterfaceControl
        val pc = proofApi.env.proofControl as AbstractProofControl

        val closed = when {
            proofFile != null -> {
                printm("[INFO] Proof found: $proofFile. Try loading.")
                loadProof(proofFile)
            }
            scriptFile != null -> {
                printm("[INFO] Script found: $scriptFile. Try proving.")
                loadScript(ui, proof, scriptFile)
            }
            else -> {
                if (verbose)
                    printm("[INFO] No proof or script found. Fallback to auto-mode.")
                runAutoMode(pc, proof)
            }
        }

        if (closed) {
            printm("[OK  ] ✔ Proof closed.", fg = GREEN)
        } else {
            errors++
            printm("[ERR ] ✘ Proof open.", fg = RED)
            if (verbose)
                printm("[FINE] ${proof.openGoals().size()} remains open")
        }
        proof.dispose()
        return closed
    }

    private fun runAutoMode(proofControl: AbstractProofControl, proof: Proof): Boolean {
        val time = measureTimeMillis {
            if (enableMeasuring) {
                val mm = MeasuringMacro()
                proofControl.runMacro(proof.root(), mm, null)
                proofControl.waitWhileAutoMode()
                printm("[INFO] Proof has open/closed before: ${mm.before}")
                printm("[INFO] Proof has open/closed after: ${mm.after}")
            } else {
                proofControl.startAndWaitForAutoMode(proof)
            }
        }
        if (verbose) {
            printm("[FINE] Auto-mode took ${time / 1000.0} seconds.")
        }
        printStatistics(proof)
        return proof.closed()
    }

    private fun loadScript(ui: AbstractUserInterfaceControl, proof: Proof, scriptFile: String): Boolean {
        val script = File(scriptFile).readText()
        val engine = ProofScriptEngine(script, Location(scriptFile, 1, 1))
        val time = measureTimeMillis {
            engine.execute(ui, proof)
        }
        print("Script execution took ${time / 1000.0} seconds.")
        printStatistics(proof)
        return proof.closed()
    }

    private fun loadProof(keyFile: String): Boolean {
        val env = KeYEnvironment.load(File(keyFile))
        try {
            val proof = env?.loadedProof
            try {
                if (proof == null) {
                    printm("[ERR] No proof found in given KeY-file.", fg = 38)
                    return false
                }
                printStatistics(proof)
                return proof.closed()
            } finally {
                proof?.dispose()
            }
        } finally {
            env.dispose()
        }
    }


    private fun printStatistics(proof: Proof) {
        if (verbose) {
            proof.statistics.summary.forEach { p -> printm("[FINE] ${p.first} = ${p.second}") }
        }
        if (enableMeasuring) {
            val closedGoals = proof.getClosedSubtreeGoals(proof.root())
            val visitLineOnClosedGoals = HashSet<Pair<String, Int>>()
            closedGoals.forEach {
                it.pathToRoot.forEach {
                    val p = it.nodeInfo.activeStatement?.positionInfo
                    if (p != null) {
                        visitLineOnClosedGoals.add(p.fileName to p.startPosition.line)
                    }
                }
            }
            printm("Visited lines:\n${visitLineOnClosedGoals.joinToString("\n")}")
        }
    }

    val proofFileCandidates by lazy {
        val candidates = ArrayList<String>()
        proofPath.forEach { candidates.addAll(File(it).list()) }
        candidates
    }

    private fun findProofFile(filename: String): String? = proofFileCandidates.find { it.startsWith(filename) && (it.endsWith(".proof") || it.endsWith(".proof.gz")) }

    private fun findScriptFile(filename: String): String? = proofFileCandidates.find { it.startsWith(filename) && (it.endsWith(".txt") || it.endsWith(".pscript")) }
}

fun main(args: Array<String>) = Checker().main(args)

private val Goal.pathToRoot: Sequence<Node>
    get() {
        return generateSequence(node()) { it.parent() }
    }

private fun Proof.openClosedProgramBranches(): Pair<Int, Int> {
    val branchingNodes = this.root().subtreeIterator().asSequence()
            .filter { it.childrenCount() > 1 }
    val programBranchingNodes = branchingNodes.filter {
        val childStmt = it.childrenIterator().asSequence().map { child ->
            child.nodeInfo.activeStatement
        }
        childStmt.any { c -> c != it.nodeInfo.activeStatement }
    }

    val diverseProgramBranches = programBranchingNodes.filter { parent ->
        !parent.isClosed && parent.childrenIterator().asSequence().any { it.isClosed }
    }

    return diverseProgramBranches.count() to programBranchingNodes.count()
}


//region Measuring
class MeasuringMacro : SequentialProofMacro() {
    val before = Stats()
    val after = Stats()

    override fun getName() = "MeasuringMacro"
    override fun getCategory() = "ci-only"
    override fun getDescription() = "like auto but with more swag"

    override fun createProofMacroArray(): Array<ProofMacro> {
        return arrayOf(
                AutoPilotPrepareProofMacro(),
                GatherStatistics(before),
                AutoMacro(), //or TryCloseMacro()?
                GatherStatistics(after)
        )
    }
}

data class Stats(var openGoals: Int = 0, var closedGoals: Int = 0)

class GatherStatistics(val stats: Stats) : SkipMacro() {
    override fun getName() = "gather-stats"
    override fun getCategory() = "ci-only"
    override fun getDescription() = "stat purpose"

    override fun canApplyTo(proof: Proof?,
                            goals: ImmutableList<Goal?>?,
                            posInOcc: PosInOccurrence?): Boolean = true

    override fun applyTo(uic: UserInterfaceControl?,
                         proof: Proof,
                         goals: ImmutableList<Goal?>?,
                         posInOcc: PosInOccurrence?,
                         listener: ProverTaskListener?): ProofMacroFinishedInfo? { // do nothing
        stats.openGoals = proof.openGoals().size()
        stats.closedGoals = proof.getClosedSubtreeGoals(proof.root()).size()
        return super.applyTo(uic, proof, goals, posInOcc, listener)
    }
}
//endregion