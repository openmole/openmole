package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Waiter, panels}
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data._
import com.raquo.laminar.api.L._
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import scala.concurrent.ExecutionContext.Implicits.global

object AnyTextContent {
  def addTab(safePath: SafePath, initialContent: String, initialHash: String) = {

    val editor = EditorPanelUI(safePath.extension, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))
    val controlElement = div(display.flex, flexDirection.row, height := "5vh", alignItems.center, TabContent.fontSizeControl, marginLeft.auto)
    val content = div(display.flex, flexDirection.column, controlElement, editor.view)

    TabContent.addTab(tabData, content)
  }
}
