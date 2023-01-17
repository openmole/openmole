package org.openmole.gui.client.core.files


import org.openmole.gui.client.core.{Fetch, Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object HTMLContent {
  
  def addTab(safePath: SafePath, content: HtmlElement)(using panels: Panels, api: ServerAPI) = {
    val tabData = TabData(safePath, None)
    panels.tabContent.addTab(tabData, content.amend(cls := "fullOverflow"))
  }
}
