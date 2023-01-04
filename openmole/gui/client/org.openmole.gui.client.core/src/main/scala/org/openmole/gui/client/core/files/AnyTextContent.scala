package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Fetch, Panels, Waiter, staticPanels}
import org.openmole.gui.ext.data.*
import org.openmole.gui.ext.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.ext.client.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object AnyTextContent {
  def addTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, fetch: Fetch) = {

    val editor = EditorPanelUI(safePath.extension, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))
    val controlElement = div(display.flex, flexDirection.row, height := "5vh", alignItems.center, panels.tabContent.fontSizeControl, marginLeft.auto)
    val content = div(display.flex, flexDirection.column, controlElement, editor.view)

    panels.tabContent.addTab(tabData, content)
  }
}
