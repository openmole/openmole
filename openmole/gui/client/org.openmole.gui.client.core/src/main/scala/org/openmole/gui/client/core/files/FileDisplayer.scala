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

  def display(safePath: SafePath)(using panels: Panels, api: ServerAPI, path: BasePath, plugins: GUIPlugins) =
    panels.tabContent.alreadyDisplayed(safePath) match
      case Some(tabID: bsn.TabID) ⇒ panels.tabContent.tabsUI.setActive(tabID)
      case _ ⇒
        FileContentType(safePath) match
          case FileContentType.OpenMOLEScript ⇒
            api.download(safePath, hash = true).foreach: (content, hash) ⇒
              OMSContent.addTab(safePath, content, hash.get)
          case FileContentType.CSV =>
            api.download(safePath, hash = true).foreach: (content, hash) ⇒
              CSVContent.addTab(safePath, content, hash.get)
          case FileContentType.MDScript ⇒
            api.mdToHtml(safePath).foreach: htmlString ⇒
              val htmlDiv = com.raquo.laminar.api.L.div()
              htmlDiv.ref.innerHTML = htmlString
              HTMLContent.addTab(safePath, htmlDiv)
          case FileContentType.OpenMOLEResult ⇒
            api.omrContent(safePath).foreach: guiContent =>
              OMRContent.addTab(safePath, guiContent.section)
          case FileContentType.SVGExtension ⇒
            api.download(safePath, hash = false).foreach: (content, _) ⇒
              HTMLContent.addTab(safePath, div(panelBody, content))
          case e: ReadableFileType if e.text =>
            api.download(safePath, hash = true).foreach: (content, hash) ⇒
              AnyTextContent.addTab(safePath, content, hash.get)
          case UnknownFileType =>
            api.isTextFile(safePath).foreach: text =>
              api.download(safePath, hash = true).foreach: (content, hash) ⇒
                if text then AnyTextContent.addTab(safePath, content, hash.get)
          case _ =>


