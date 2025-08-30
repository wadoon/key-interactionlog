/* This file is part of key-interactionlog.
 * key-interactionlog is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
@file:Suppress("unused")

package io.github.wadoon.key.interactionlog

import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboPopup
import kotlin.math.max
import kotlin.math.min

/**
 * This class will change the bounds of the JComboBox popup menu to support
 * different functionality. It will support the following features:
 * -  a horizontal scrollbar can be displayed when necessary
 * -  the popup can be wider than the combo box
 * -  the popup can be displayed above the combo box
 *
 * Class will only work for a JComboBox that uses a BasicComboPop.
 *
 * @param scrollBarRequired display a horizontal scrollbar when the
 * preferred width of popup is greater than width of scrollPane.
 * @param popupWider display the popup at its preferred with
 * @param maximumWidth limit the popup width to the value specified
 * (minimum size will be the width of the combo box)
 * @param popupAbove display the popup above the combo box
 * @see <https:></https:>//tips4java.wordpress.com/2010/11/28/combo-box-popup/>
 */
open class BoundsPopupMenuListener @JvmOverloads constructor(
    scrollBarRequired: Boolean = true,
    popupWider: Boolean = false,
    maximumWidth: Int = -1,
    popupAbove: Boolean = false,
) : PopupMenuListener {
    /**
     * Determine if the horizontal scroll bar might be required for the popup
     *
     * @return the scrollBarRequired value
     */
    /**
     * For some reason the default implementation of the popup removes the
     * horizontal scrollBar from the popup scroll pane which can result in
     * the truncation of the rendered items in the popop. Adding a scrollBar
     * back to the scrollPane will allow horizontal scrolling if necessary.
     *
     * @param scrollBarRequired  true add horizontal scrollBar to scrollPane
     * false remove the horizontal scrollBar
     */
    var isScrollBarRequired: Boolean = true
    /**
     * Determine if the popup might be displayed wider than the combo box
     *
     * @return the popupWider value
     */

    /**
     * Change the width of the popup to be the greater of the width of the
     * combo box or the preferred width of the popup. Normally the popup width
     * is always the same size as the combo box width.
     *
     * @param popupWider  true adjust the width as required.
     */
    var isPopupWider: Boolean = false
    /**
     * Return the maximum width of the popup.
     *
     * @return the maximumWidth value
     */

    /**
     * Set the maximum width for the popup. This value is only used when
     * setPopupWider( true ) has been specified. A value of -1 indicates
     * that there is no maximum.
     *
     * @param maximumWidth  the maximum width of the popup
     */
    var maximumWidth: Int = -1
    /**
     * Determine if the popup should be displayed above the combo box.
     *
     * @return the popupAbove value
     */

    /**
     * Change the location of the popup relative to the combo box.
     *
     * @param popupAbove  true display popup above the combo box,
     * false display popup below the combo box.
     */
    var isPopupAbove: Boolean = false

    private var scrollPane: JScrollPane? = null

    /**
     * Convenience constructor to allow the display of a horizontal scrollbar
     * when required.
     */
    init {
        this.isScrollBarRequired = scrollBarRequired
        this.isPopupWider = popupWider
        this.maximumWidth = maximumWidth
        this.isPopupAbove = popupAbove
    }

    /**
     * Alter the bounds of the popup just before it is made visible.
     */
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        val comboBox = e.getSource() as JComboBox<*>

        if (comboBox.itemCount == 0) return

        val child: Any? = comboBox.getAccessibleContext().getAccessibleChild(0)

        if (child is BasicComboPopup) {
            SwingUtilities.invokeLater { customizePopup(child) }
        }
    }

    protected fun customizePopup(popup: BasicComboPopup) {
        scrollPane = getScrollPane(popup)

        if (this.isPopupWider) popupWider(popup)

        checkHorizontalScrollBar(popup)

        //  For some reason in JDK7 the popup will not display at its preferred
        //  width unless its location has been changed from its default
        //  (ie. for normal "pop down" shift the popup and reset)
        val comboBox = popup.getInvoker()
        val location = comboBox.locationOnScreen

        if (this.isPopupAbove) {
            val height = popup.preferredSize.height
            popup.setLocation(location.x, location.y - height)
        } else {
            val height = comboBox.preferredSize.height
            popup.setLocation(location.x, location.y + height - 1)
            popup.setLocation(location.x, location.y + height)
        }
    }

    /*
     *  Adjust the width of the scrollpane used by the popup
     */
    protected fun popupWider(popup: BasicComboPopup) {
        val list: JList<*> = popup.getList()

        //  Determine the maximimum width to use:
        //  a) determine the popup preferred width
        //  b) limit width to the maximum if specified
        //  c) ensure width is not less than the scroll pane width
        var popupWidth = (
            list.preferredSize.width +
                5 + // make sure horizontal scrollbar doesn't appear
                getScrollBarWidth(popup, scrollPane!!)
            )

        if (maximumWidth != -1) {
            popupWidth = min(popupWidth, maximumWidth)
        }

        val scrollPaneSize = scrollPane!!.preferredSize
        popupWidth = max(popupWidth, scrollPaneSize.width)

        //  Adjust the width
        scrollPaneSize.width = popupWidth
        scrollPane!!.preferredSize = scrollPaneSize
        scrollPane!!.maximumSize = scrollPaneSize
    }

    /*
     *  This method is called every time:
     *  - to make sure the viewport is returned to its default position
     *  - to remove the horizontal scrollbar when it is not wanted
     */
    private fun checkHorizontalScrollBar(popup: BasicComboPopup) {
        //  Reset the viewport to the left

        val viewport = scrollPane!!.getViewport()
        val p = viewport.viewPosition
        p.x = 0
        viewport.viewPosition = p

        //  Remove the scrollbar so it is never painted
        if (!this.isScrollBarRequired) {
            scrollPane!!.setHorizontalScrollBar(null)
            return
        }

        // 	Make sure a horizontal scrollbar exists in the scrollpane
        var horizontal = scrollPane!!.getHorizontalScrollBar()

        if (horizontal == null) {
            horizontal = JScrollBar(JScrollBar.HORIZONTAL)
            scrollPane!!.setHorizontalScrollBar(horizontal)
            scrollPane!!.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        }

        // 	Potentially increase height of scroll pane to display the scrollbar
        if (horizontalScrollBarWillBeVisible(popup, scrollPane!!)) {
            val scrollPaneSize = scrollPane!!.preferredSize
            scrollPaneSize.height += horizontal.preferredSize.height
            scrollPane!!.preferredSize = scrollPaneSize
            scrollPane!!.maximumSize = scrollPaneSize
            scrollPane!!.revalidate()
        }
    }

    /*
     *  Get the scroll pane used by the popup so its bounds can be adjusted
     */
    protected fun getScrollPane(popup: BasicComboPopup): JScrollPane? {
        val list: JList<*> = popup.getList()
        val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, list)

        return c as JScrollPane?
    }

    /*
     *  I can't find any property on the scrollBar to determine if it will be
     *  displayed or not so use brute force to determine this.
     */
    protected fun getScrollBarWidth(popup: BasicComboPopup, scrollPane: JScrollPane): Int {
        var scrollBarWidth = 0
        val comboBox = popup.getInvoker() as JComboBox<*>

        if (comboBox.itemCount > comboBox.getMaximumRowCount()) {
            val vertical = scrollPane.getVerticalScrollBar()
            scrollBarWidth = vertical.preferredSize.width
        }

        return scrollBarWidth
    }

    /*
     *  I can't find any property on the scrollBar to determine if it will be
     *  displayed or not so use brute force to determine this.
     */
    protected fun horizontalScrollBarWillBeVisible(popup: BasicComboPopup, scrollPane: JScrollPane): Boolean {
        val list: JList<*> = popup.getList()
        val scrollBarWidth = getScrollBarWidth(popup, scrollPane)
        val popupWidth = list.preferredSize.width + scrollBarWidth

        return popupWidth > scrollPane.preferredSize.width
    }

    override fun popupMenuCanceled(e: PopupMenuEvent?) {}

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
        //  In its normal state the scrollpane does not have a scrollbar

        if (scrollPane != null) {
            scrollPane!!.setHorizontalScrollBar(null)
        }
    }
}
