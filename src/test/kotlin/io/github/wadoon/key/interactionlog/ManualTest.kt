package io.github.wadoon.key.interactionlog

import de.uka.ilkd.key.gui.MainWindow
import javax.swing.SwingUtilities

object ManualTest {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            val w = MainWindow.getInstance()
            w.isVisible = true
        }
    }
}