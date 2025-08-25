/* This file is part of key-abbrevmgr.
 * key-abbrevmgr is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
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
