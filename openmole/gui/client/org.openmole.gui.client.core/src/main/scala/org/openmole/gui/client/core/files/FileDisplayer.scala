package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.core.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.plot.Plotter
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.bsn
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.HTMLDivElement

import scala.concurrent.Future

/*
 * Copyright (C) 07/05/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object FileDisplayer:

  def buildTab(safePath: SafePath)(using panels: Panels, api: ServerAPI, path: BasePath, plugins: GUIPlugins): Future[Option[(TabData, HtmlElement)]] =
    FileContentType(safePath) match
      case FileContentType.OpenMOLEScript ⇒
        api.download(safePath, hash = true).map: (content, hash) ⇒
          Some(OMSContent.buildTab(safePath, content, hash.get))
      case FileContentType.CSV =>
        api.download(safePath, hash = true).map: (content, hash) ⇒
          Some(CSVContent.buildTab(safePath, content, hash.get))
      case FileContentType.MDScript ⇒
        api.mdToHtml(safePath).map: htmlString ⇒
          val htmlDiv = com.raquo.laminar.api.L.div()
          htmlDiv.ref.innerHTML = htmlString
          Some(HTMLContent.buildTab(safePath, htmlDiv))
      case FileContentType.OpenMOLEResult ⇒
        api.omrContent(safePath).map: guiContent =>
          Some(OMRContent.buildTab(safePath, guiContent))
      case FileContentType.SVGExtension ⇒
        api.download(safePath, hash = false).map: (content, _) ⇒
          Some(HTMLContent.buildTab(safePath, div(panelBody, content)))
      case e: ReadableFileType if e.text =>
        api.download(safePath, hash = true).map: (content, hash) ⇒
          Some(AnyTextContent.buildTab(safePath, content, hash.get))
      case UnknownFileType =>
        api.isTextFile(safePath).flatMap: text =>
          api.download(safePath, hash = true).map: (content, hash) ⇒
            if text
            then
              Some(AnyTextContent.buildTab(safePath, content, hash.get))
            else None
      case _ => Future.successful(None)


  def display(safePath: SafePath)(using panels: Panels, api: ServerAPI, path: BasePath, plugins: GUIPlugins) =
    panels.tabContent.alreadyDisplayed(safePath) match
      case Some(tabID: bsn.TabID) ⇒ panels.tabContent.tabsUI.setActive(tabID)
      case _ ⇒
        val tab = buildTab(safePath)
        tab.foreach:
          case Some((tabData, content)) => 
            panels.tabContent.addTab(tabData, content)
          case _ => 

