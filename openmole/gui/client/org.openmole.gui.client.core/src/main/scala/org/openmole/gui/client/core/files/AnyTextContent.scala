package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.Component
import org.openmole.gui.shared.api.*
import org.openmole.gui.client.tool.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object AnyTextContent:

  def buildTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
    val editor = EditorPanelUI(safePath, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))
    val content = div(display.flex, flexDirection.column, editor.view)
    (tabData, content)