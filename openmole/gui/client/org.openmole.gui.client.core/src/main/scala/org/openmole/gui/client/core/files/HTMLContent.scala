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

object HTMLContent {
  
  def addTab(safePath: SafePath, content: HtmlElement) = {
    val tabData = TabData(safePath, None)
    TabContent.addTab(tabData, content.amend(cls := "fullOverflow"))
  }
}
