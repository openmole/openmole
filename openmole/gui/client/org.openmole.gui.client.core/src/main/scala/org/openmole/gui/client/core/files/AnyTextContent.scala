package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.Component
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object AnyTextContent:

  def buildTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
    val editor = EditorPanelUI(safePath, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))
    val controlElement = div(display.flex, flexDirection.row, height := "5vh", alignItems.center, panels.tabContent.fontSizeControl, marginLeft.auto)
    val content = div(display.flex, flexDirection.column, controlElement, editor.view)
    (tabData, content)


