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

  def apply(safePath: SafePath, fileType: FileExtension, initCode: String, containerModifierSeq: ModifierSeq = emptyMod) =
    fileType match {
      case OMS   ⇒ editor(safePath, initCode, OMS, containerModifierSeq)
      case SCALA ⇒ editor(safePath, initCode, SCALA, containerModifierSeq)
      case _     ⇒ empty(safePath, initCode, containerModifierSeq)
    }

  def empty(
    safePath:             SafePath,
    initCode:             String,
    containerModifierSeq: ModifierSeq) = new EditorPanelUI(safePath, initCode, NO_EXTENSION, containerModifierSeq)

  private def editor(safePath: SafePath, initCode: String = "", language: FileExtension, containerModifierSeq: ModifierSeq) =
    new EditorPanelUI(safePath, initCode, language, containerModifierSeq)

}

class EditorPanelUI(safePath: SafePath, initCode: String, fileType: FileExtension, containerModifierSeq: ModifierSeq) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  lazy val editorDiv = tags.div(id := "editor").render

  lazy val editor = {
    val ed = ace.edit(editorDiv)

    fileType match {
      case ef: HighlightedFile ⇒ ed.getSession().setMode("ace/mode/" + ef.highlighter)
      case _                   ⇒
    }

    ed.getSession().setValue(initCode)
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

              if (extension == OMS && TreeNodeTabs.isActive(org.openmole.gui.client.core.panels.treeNodeTabs, safePath)) {
                div(
                  for {
                    i ← TreeNodeTabs.errorsInEditor(safePath)().filter { e ⇒
                      e > scrollAsLines && e < max
                    }
                  } yield {
                    TreeNodeTabs.errors(safePath)().find(_.errorWithLocation.line == Some(i)).map { e ⇒
                      e.errorWithLocation.line.map { l ⇒
                        buildManualPopover(l, (i - scrollAsLines) * lineHeight() - (lineHeight() - 15), span(e.errorWithLocation.stackTrace), Popup.Left)
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
  def code: String = editor.getSession().getValue()

  def setCode(content: String) = editor.getSession().setValue(content)

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

