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
import org.openmole.gui.ext.tool.client._
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

class EditorPanelUI(safePath: SafePath, initCode: String, fileType: FileExtension, containerModifierSeq: ModifierSeq) {

  val editorDiv = tags.div(id := "editor").render
  val editor = ace.edit(editorDiv)

  val lineHeight = Var(15)
  val scrollTop = Var(0.0)
  val changed = Var(false)

  def fontDimension = lineHeight.now - 3

  def updateFont(lHeight: Int) = {
    lineHeight.update(lHeight)
  }

  lineHeight.trigger {
    editor.container.style.lineHeight = s"${lineHeight.now}px"
    editor.container.style.fontSize = s"${fontDimension}px"
    editor.renderer.updateFontSize
    ()
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
              if (extension == OMS && org.openmole.gui.client.core.panels.treeNodeTabs.isActive(safePath)() == TreeNodeTabs.Active) {
                div(
                  for {
                    i ← errorsInEditor().filter { e ⇒
                      e > scrollAsLines && e < max
                    }
                  } yield {
                    errors().find(_.errorWithLocation.line == Some(i)).map { e ⇒
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

  def errors = TreeNodeTabs.errors(safePath)

  def errorsInEditor = TreeNodeTabs.errorsInEditor(safePath)

  def setErrors(errorsWithLocation: Seq[ErrorWithLocation]): Unit = {
    changed.update(false)
    TreeNodeTabs.updateErrors(safePath, errorsWithLocation.map { ewl ⇒ ErrorFromCompiler(ewl, ewl.line.map { l ⇒ session.doc.getLine(l) }.getOrElse("")) })
    TreeNodeTabs.updateErrorsInEditor(safePath, errorsWithLocation.flatMap {
      _.line
    })

  }

  def session = editor.getSession()

  def aceDoc = session.getDocument()

  def code: String = session.getValue()

  def setCode(content: String) = editor.getSession().setValue(content)

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  def updateScrollTop = scrollTop.update(editor.renderer.getScrollTop)

  session.on("change", (x) ⇒ {
    changed.update(true)
    updateScrollTop
  })

  session.on("changeScrollTop", x ⇒ {
    updateScrollTop
  })

  def buildManualPopover(line: Int, topPosition: Double, title: String, position: PopupPosition) = {
    lazy val pop1 = div(line)(`class` := "gutterError", fontSize := s"${fontDimension}px", top := topPosition, height := s"${lineHeight.now + 3}px", width := s"${lineHeight.now + 5}px",
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

  def initEditor = {
    fileType match {
      case ef: HighlightedFile ⇒ editor.getSession().setMode("ace/mode/" + ef.highlighter)
      case _                   ⇒
    }

    setCode(initCode)
    editor.setTheme("ace/theme/github")
    ace.require("ace/ext/language_tools")
    editor.renderer.setShowGutter(true)
    editor.setShowPrintMargin(true)
    editor.setAutoScrollEditorIntoView(true)

    editor.setOptions(js.Dynamic.literal(
      "enableBasicAutocompletion" -> true,
      "enableSnippets" -> true,
      "enableLiveAutocompletion" -> true
    ))
  }

}

object EditorPanelUI {

  def apply(safePath: SafePath, fileType: FileExtension, initCode: String, containerModifierSeq: ModifierSeq = emptyMod) = fileType match {
    case OMS   ⇒ editor(safePath, initCode, OMS, containerModifierSeq)
    case SCALA ⇒ editor(safePath, initCode, SCALA, containerModifierSeq)
    case _     ⇒ empty(safePath, initCode, containerModifierSeq)
  }

  def empty(safePath: SafePath, initCode: String, containerModifierSeq: ModifierSeq) = new EditorPanelUI(safePath, initCode, NO_EXTENSION, containerModifierSeq)

  private def editor(safePath: SafePath, initCode: String = "", language: FileExtension, containerModifierSeq: ModifierSeq = emptyMod) = new EditorPanelUI(safePath, initCode, language, containerModifierSeq)

  //def sh(initCode: String = "") = EditorPanelUI(SH, initCode)

}