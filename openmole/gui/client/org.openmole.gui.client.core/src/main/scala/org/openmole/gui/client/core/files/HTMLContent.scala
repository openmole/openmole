package org.openmole.gui.client.core.files


import org.openmole.gui.client.core.{Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object HTMLContent:

  def buildTab(safePath: SafePath, content: HtmlElement)(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    val tabData = TabData(safePath, None)
    (tabData, content.amend(cls := "fullOverflow"))
