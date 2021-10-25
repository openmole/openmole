package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._

import scala.scalajs.js
import scaladget.ace._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data.DataUtils._

import scala.scalajs.js.JSConverters._
import org.openmole.gui.ext.client._
import org.scalajs.dom.raw.Event
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.{Manual, PopupPosition}
import com.raquo.laminar.api.L._
import com.raquo.laminar.builders.DomEventStreamPropBuilder
import org.openmole.gui.client.core.panels
import org.openmole.gui.ext.data

import scala.scalajs.js.annotation.JSImport

/*
 * Copyright (C) 07/04/15 // mathieu.leclaire@openmole.org
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

object EditorPanelUI {

  def apply(
             treeNodeTabs: TreeNodeTabs,
             safePath: SafePath,
             fileType: FileExtension,
             initCode: String,
             initHash: String,
             //containerHESetters: HESetters     = emptySetters
           ) = {
    val editor = {
      fileType match {
        case OMS ⇒ new EditorPanelUI(treeNodeTabs, safePath, OMS)
        case SCALA ⇒ new EditorPanelUI(treeNodeTabs, safePath, SCALA)
        case _ ⇒ new EditorPanelUI(treeNodeTabs, safePath, NO_EXTENSION)
      }
    }
    editor.setCode(initCode, initHash)
    editor
  }

  def highlightedFile(ext: FileExtension): Option[HighlightedFile] =
    ext match {
      case OpenMOLEScript ⇒ Some(HighlightedFile("openmole"))
      case e: EditableFile ⇒ Some(HighlightedFile(e.highlighter))
      case _ ⇒ None
    }

  case class HighlightedFile(highlighter: String)

  @js.native
  @JSImport("ace-builds/src-noconflict/mode-openmole.js", JSImport.Namespace)
  object openmolemode extends js.Object
}

class EditorPanelUI(treeNodeTabs: TreeNodeTabs, safePath: SafePath, fileType: FileExtension) {

  val edDiv = div(idAttr := "editor")
  val editor = {
    val ed = ace.edit(edDiv.ref)

    js.Dynamic.global.ace.config.set("basePath", "js")
    js.Dynamic.global.ace.config.set("modePath", "js")
    js.Dynamic.global.ace.config.set("themePath", "js")

    scalamode
    EditorPanelUI.openmolemode
    githubtheme
    extLanguageTools

    ed.setTheme("ace/theme/github")

    EditorPanelUI.highlightedFile(fileType).foreach { h ⇒
      ed.getSession().setMode("ace/mode/" + h.highlighter)
    }

    ed.renderer.setShowGutter(true)
    ed.setShowPrintMargin(true)
    ed.setAutoScrollEditorIntoView(false)

    ed.setOptions(
      js.Dynamic.literal(
        "enableBasicAutocompletion" -> true,
        "enableSnippets" -> true,
        "enableLiveAutocompletion" -> true
      )
    )

    //    def updateScrollTop = scrollTop.set(ed.renderer.getScrollTop)
    //
    //    ed.getSession().on("change", (x) ⇒ {
    //      changed.set(true)
    //      updateScrollTop
    //    })
    //
    //    ed.getSession().on("changeScrollTop", x ⇒ {
    //      updateScrollTop
    //    })
    //
   // ed.resize()
    ed
  }

  var initialContentHash = ""

  lazy val lineHeight = Var(15)

  val lineHeightObserver = Observer[Int] { (i: Int) ⇒
    editor.container.style.lineHeight = s"${i}px"
    editor.container.style.fontSize = s"${i - 3}px"
    editor.renderer.updateFontSize
  }

  lazy val scrollTop = Var(0.0)
  lazy val changed = Var(false)

  def updateFont(lHeight: Int) = {
    lineHeight.set(lHeight)
  }

  //val extension: FileExtension = FileExtension(treeNodeTab.safePathTab.now.name)

  lazy val view = {
    edDiv.amend(
      div(
        cls := "gutterDecoration",
        child <-- scrollTop.signal.combineWith(lineHeight.signal).map {
          case (sTop, lHeight) ⇒
            val scrollAsLines = sTop / lHeight
            val max = editor.renderer.getLastVisibleRow

            //                def isActive = {
            //                  treeNodeTabs.isActive(safePath) == TreeNodeTabs.Active
            //                }

            treeNodeTabs.tab(safePath).map {
              _.t
            } match {
              case Some(oms: TreeNodeTab.OMS) ⇒
                if (treeNodeTabs.isActive(safePath)) {
                  div(
                    children <--
                      oms.errors.signal.map { es ⇒
                        es.errorsInEditor.map { i ⇒
                          es.errorsFromCompiler.find(_.errorWithLocation.line == Some(i)).map { e ⇒
                            e.errorWithLocation.line.map { l ⇒
                              buildManualPopover(l, /*(i - scrollAsLines) * lHeight - (lHeight - 15)*/ lHeight, e.errorWithLocation.stackTrace, Popup.Right)
                            }.getOrElse(div())
                          }.getOrElse(div())
                        }
                      }
                  )
                }
                else div()
              case _ ⇒ div()
            }
        },
        lineHeight --> lineHeightObserver
      )
    )
  }

  def aceDoc = editor.getSession().getDocument()

  def code = editor.synchronized {
    (editor.getSession().getValue(), initialContentHash)
  }

  def setCode(content: String, hash: String) = editor.synchronized {
    initialContentHash = hash
    editor.getSession().setValue(content)
  }

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  def buildManualPopover(line: Int, topPosition: Double, title: String, position: PopupPosition) = {
    lazy val manualPopover: PopoverBuilder = div(line, cls := "gutterError",
      fontSize <-- lineHeight.signal.map { lh ⇒ s"${lh - 3}px" },
      top := topPosition.toString, height := s"${lineHeight.now + 3}px", width := s"${lineHeight.now + 5}px",
      backgroundColor <-- changed.signal.map { c ⇒
        if (c) "rgba(255,204,0)"
        else "rgba(255,128,128)"
      },
      onClick --> { e ⇒
        manualPopover.hide
        manualPopover.toggle
        e.stopPropagation
      }
    ).popover(
      span(title),
      position,
      Manual
    )

    div(manualPopover.render)
  }

}

