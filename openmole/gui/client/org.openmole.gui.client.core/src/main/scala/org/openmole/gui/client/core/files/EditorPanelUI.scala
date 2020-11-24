package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._

import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.tags

import scaladget.ace._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data.DataUtils._

import scala.scalajs.js.JSConverters._
import org.openmole.gui.ext.client._
import org.scalajs.dom.raw.Event
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.{ Manual, PopupPosition }
import rx._

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
    treeNodeTabs:         TreeNodeTabs,
    safePath:             SafePath,
    fileType:             FileExtension,
    initCode:             String,
    initHash:             String,
    containerModifierSeq: ModifierSeq   = emptyMod) = {
    val editor = {
      fileType match {
        case OMS   ⇒ new EditorPanelUI(treeNodeTabs, safePath, OMS, containerModifierSeq)
        case SCALA ⇒ new EditorPanelUI(treeNodeTabs, safePath, SCALA, containerModifierSeq)
        case _     ⇒ new EditorPanelUI(treeNodeTabs, safePath, NO_EXTENSION, containerModifierSeq)
      }
    }
    editor.setCode(initCode, initHash)
    editor
  }

  def highlightedFile(ext: FileExtension): Option[HighlightedFile] =
    ext match {
      case OpenMOLEScript  ⇒ Some(HighlightedFile("openmole"))
      case e: EditableFile ⇒ Some(HighlightedFile(e.highlighter))
      case _               ⇒ None
    }

  case class HighlightedFile(highlighter: String)
}

class EditorPanelUI(treeNodeTabs: TreeNodeTabs, safePath: SafePath, fileType: FileExtension, containerModifierSeq: ModifierSeq) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  lazy val editorDiv = tags.div(id := "editor").render

  var initialContentHash = ""

  lazy val editor = {
    val ed = ace.edit(editorDiv)

    EditorPanelUI.highlightedFile(fileType).foreach { h ⇒
      ed.getSession().setMode("ace/mode/" + h.highlighter)
    }

    ed.setTheme("ace/theme/github")
    ace.require("ace/ext/language_tools")
    ed.renderer.setShowGutter(true)
    ed.setShowPrintMargin(true)
    ed.setAutoScrollEditorIntoView(true)

    ed.setOptions(js.Dynamic.literal(
      "enableBasicAutocompletion" -> true,
      "enableSnippets" -> true,
      "enableLiveAutocompletion" -> true
    ))

    def updateScrollTop = scrollTop.update(ed.renderer.getScrollTop)

    ed.getSession().on("change", (x) ⇒ {
      changed.update(true)
      updateScrollTop
    })

    ed.getSession().on("changeScrollTop", x ⇒ { updateScrollTop })
    ed
  }

  lazy val lineHeight = {
    val h = Var(15)
    h.trigger { v ⇒
      editor.container.style.lineHeight = s"${v}px"
      editor.container.style.fontSize = s"${v - 3}px"
      editor.renderer.updateFontSize
      ()
    }
    h
  }

  def fontDimension = lineHeight.now - 3

  lazy val scrollTop = Var(0.0)
  lazy val changed = Var(false)

  def updateFont(lHeight: Int) = {
    lineHeight.update(lHeight)
  }

  val extension: FileExtension = FileExtension(safePath.name)

  lazy val view = {
    div(editorContainer +++ container +++ containerModifierSeq)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          editor.container,
          div(`class` := "gutterDecoration")(
            Rx {
              val scrollAsLines = scrollTop() / lineHeight()
              val max = editor.renderer.getLastVisibleRow

              def isActive =
                treeNodeTabs.isActive(safePath)() == TreeNodeTabs.Active

              if (extension == OMS && isActive) {
                val errors = TreeNodeTabs.errors(treeNodeTabs, safePath)()

                div(
                  for {
                    i ← errors.errorsInEditor.filter { e ⇒ e > scrollAsLines && e < max }
                  } yield {
                    errors.errorsFromCompiler.find(_.errorWithLocation.line == Some(i)).map { e ⇒
                      e.errorWithLocation.line.map { l ⇒
                        buildManualPopover(l, (i - scrollAsLines) * lineHeight() - (lineHeight() - 15), span(e.errorWithLocation.stackTrace), Popup.Right)
                      }.getOrElse(div.render)
                    }.getOrElse(div.render)
                  }
                )
              }
              else div("")

            }).render
        )
      )
    )
  }

  def aceDoc = editor.getSession().getDocument()

  def code = editor.synchronized { (editor.getSession().getValue(), initialContentHash) }

  def setCode(content: String, hash: String) = editor.synchronized {
    initialContentHash = hash
    editor.getSession().setValue(content)
  }

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  def buildManualPopover(line: Int, topPosition: Double, title: String, position: PopupPosition) = {
    lazy val pop1 =
      div(line)(`class` := "gutterError", fontSize := s"${fontDimension}px", top := topPosition, height := s"${lineHeight.now + 3}px", width := s"${lineHeight.now + 5}px",
        backgroundColor := Rx {
          if (changed()) "rgba(255,204,0)"
          else "rgba(255,128,128)"
        }
      ).popover(
          title,
          position,
          Manual
        )

    lazy val pop1Render = pop1.render

    pop1Render.onclick = { (e: Event) ⇒
      Popover.hide
      Popover.toggle(pop1)
      e.stopPropagation
    }
    pop1Render
  }

}

