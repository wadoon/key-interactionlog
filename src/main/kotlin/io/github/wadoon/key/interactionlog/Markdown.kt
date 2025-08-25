/* This file is part of key-abbrevmgr.
 * key-abbrevmgr is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
package io.github.wadoon.key.interactionlog

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.util.*

/**
 * A facade to the world of Markdown.
 *
 * @author Alexander Weigl
 * @version 1 (07.12.18)
 */
object Markdown {
    fun html(markdown: String): String {
        val extensions = Arrays.asList(TablesExtension.create())
        val parser = Parser.builder()
                .extensions(extensions)
                .build()
        val renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build()
        val node = parser.parse(markdown)
        return renderer.render(node)
    }
}
