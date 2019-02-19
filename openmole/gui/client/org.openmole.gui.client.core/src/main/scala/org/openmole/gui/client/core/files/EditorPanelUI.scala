package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._

import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

import scala.async.Async.{ async, await }
import scaladget.ace._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data.DataUtils._

import scala.scalajs.js.JSConverters._
import org.openmole.gui.ext.tool.client._
import org.scalajs.dom.raw.{ Element, Event, HTMLDivElement, HTMLElement }
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.{ ClickPopup, Manual, PopupPosition }
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

  def save(onsave: () ⇒ Unit) = {}

  val editorDiv = tags.div(id := "editor").render
  val editor = ace.edit(editorDiv)
  val offset = Var(0)

  val extension: FileExtension = safePath.name
  lazy val view = {
    div(editorContainer +++ container +++ containerModifierSeq)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          editor.container,
          div(`class` := "gutterDecoration")(
            Rx {
              if (extension == OMS && org.openmole.gui.client.core.panels.treeNodeTabs.isActive(safePath)() == TreeNodeTabs.Active) {
                val range = (nbLines()._1 until nbLines()._2)
                div(marginTop := offset())(
                  for (
                    r ← range
                  ) yield {
                    if (errorsInEditor().exists(_ == r)) {
                      errors().find(e ⇒ e.errorWithLocation.line == Some(r)).map { e ⇒
                        e.errorWithLocation.line.map { l ⇒
                          buildManualPopover(l, e.errorWithLocation.stackTrace, Popup.Left)
                        }.getOrElse(buildCleanGutter(r, correctedInEditor()))
                      }.getOrElse(buildCleanGutter(r, correctedInEditor()))
                    }
                    else {
                      buildCleanGutter(r, correctedInEditor())
                    }
                  }
                )
              }
              else div("")

            }).render
        )
      )
    )
  }

  val nbLines: Var[(Int, Int)] = Var((editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt))

  def errors = TreeNodeTabs.errors(safePath)

  def errorsInEditor = TreeNodeTabs.errorsInEditor(safePath)

  def correctedInEditor = errors.map { e ⇒
    e.flatMap {
      _.errorWithLocation.line
    }.filterNot {
      errorsInEditor.now.contains
    }
  }

  def setErrors(errorsWithLocation: Seq[ErrorWithLocation]): Unit = {
    TreeNodeTabs.updateErrors(safePath, errorsWithLocation.map { ewl ⇒ ErrorFromCompiler(ewl, ewl.line.map { l ⇒ session.doc.getLine(l) }.getOrElse("")) })
    TreeNodeTabs.updateErrorsInEditor(safePath, errorsWithLocation.flatMap {
      _.line
    })

    setNbLines
  }

  def session = editor.getSession()

  def aceDoc = session.getDocument()

  def code: String = session.getValue()

  def setCode(content: String) = editor.getSession().setValue(content)

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  def setNbLines = {
    nbLines() = (editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt)
  }

  //MEMORY LEAK SNIPPET
  //  session.on("change", (x) ⇒ {
  //    nbLines() = (editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt)
  //  })
  //
  //  session.on("changeScrollTop", x ⇒ {
  //    Popover.current.now.foreach { p ⇒
  //      Popover.toggle(p)
  //    }
  //    nbLines() = (editor.renderer.getScrollTopRow.toInt, editor.renderer.getScrollBottomRow.toInt)
  //
  //    val nbL = code.count((c: Char) ⇒ c == '\n')
  //    offset() = {
  //      if (editor.renderer.getScrollBottomRow().toInt == nbL) -10
  //      else if (editor.renderer.getScrollTopRow().toInt == 0) 0
  //      else offset.now
  //    }
  //  })

  //  editor.session.doc.on("change", x ⇒ {
  //    if (extension == OMS && org.openmole.gui.client.core.panels.treeNodeTabs.isActive(safePath).now == TreeNodeTabs.Active) {
  //      val currentPosition = editor.getCursorPosition.row.toInt + 1
  //      if (errorsInEditor.now.contains(currentPosition)) {
  //        TreeNodeTabs.updateErrorsInEditor(safePath, errorsInEditor.now.filterNot(_ == currentPosition))
  //      }
  //      else {
  //        val cor = correctedInEditor.filter {
  //          _ == currentPosition
  //        }
  //        // Error cache
  //        val errText = errors.now.filter {
  //          _.errorWithLocation.line == Some(currentPosition)
  //        }.map {
  //          _.lineContent
  //        }.headOption.getOrElse("")
  //        cor.foreach { c ⇒
  //          if (errText == session.doc.getLine(currentPosition)) {
  //            TreeNodeTabs.updateErrorsInEditor(safePath, errorsInEditor.now :+ currentPosition)
  //          }
  //        }
  //      }
  //    }
  //  })

  def buildManualPopover(i: Int, title: String, position: PopupPosition) = {
    lazy val pop1 = div(i)(`class` := "gutterError", height := 15).popover(
      title,
      position,
      Manual
    )
    lazy val pop1Render = pop1.render

    pop1Render.onclick = { (e: Event) ⇒
      if (Popover.current.now == pop1) Popover.hide
      else {
        Popover.current.now.foreach { p ⇒
          Popover.toggle(p)
        }
        Popover.toggle(pop1)
      }
      e.stopPropagation
    }
    pop1Render
  }

  def buildCleanGutter(row: Int, corrects: Seq[Int]) = {
    if (corrects.contains(row)) div(`class` := "gutterCorrected", height := 15)(row).render
    else div(height := 15, opacity := 0).render
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