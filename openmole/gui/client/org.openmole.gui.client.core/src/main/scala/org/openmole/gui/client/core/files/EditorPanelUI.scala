package org.openmole.gui.client.core.files

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

class EditorPanelUI(initCode: String, fileType: FileExtension, containerModifierSeq: ModifierSeq) {

  def save(onsave: () ⇒ Unit) = {}

  val editorDiv = tags.div(id := "editor").render
  val editor = ace.edit(editorDiv)

  lazy val view = {
    div(editorContainer +++ container +++ containerModifierSeq)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          editor.container,
          errorDiv
        )
      )
    )
  }

  val errors: Var[Seq[(ErrorWithLocation, String)]] = Var(Seq())
  val errorsInEditor: Var[Seq[Int]] = Var(Seq())
  val correctedInEditor: Var[Seq[Int]] = Var(Seq())

  def session = editor.getSession()

  def aceDoc = session.getDocument()

  def code: String = session.getValue()

  def setCode(content: String) = editor.getSession().setValue(content)

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  val nbLines: Var[(Int, Int)] = Var((editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt))

  session.on("change", (x) ⇒ {
    nbLines() = (editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt)
  })

  session.on("changeScrollTop", x ⇒ {
    Popover.current.now.foreach { p ⇒
      Popover.toggle(p)
    }
    nbLines() = (editor.renderer.getScrollTopRow.toInt, editor.renderer.getScrollBottomRow.toInt)
  })

  editor.session.doc.on("change", x ⇒ {
    val currentPosition = editor.getCursorPosition.row.toInt + 1
    if (errorsInEditor.now.contains(currentPosition)) {
      errorsInEditor.update(errorsInEditor.now.filterNot(_ == currentPosition))
      correctedInEditor.update((correctedInEditor.now :+ currentPosition).distinct)
    }
    else {
      val cor = correctedInEditor.now.filter {
        _ == currentPosition
      }
      // Error cache
      val errText = errors.now.filter {
        _._1.line == Some(currentPosition)
      }.map {
        _._2
      }.headOption.getOrElse("")
      cor.foreach { c ⇒
        if (errText == session.doc.getLine(currentPosition)) {
          correctedInEditor.update(correctedInEditor.now.filterNot(_ == currentPosition))
          errorsInEditor.update((errorsInEditor.now :+ currentPosition).distinct)
        }
      }
    }
  })

  def buildManualPopover(i: Int, title: String, position: PopupPosition) = {
    lazy val pop1 = div(i)(`class` := "gutterError").popover(
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
    if (corrects.contains(row)) div(`class` := "gutterCorrected").render
    else div(height := 15, opacity := 0).render
  }

  lazy val errorDiv: TypedTag[HTMLDivElement] = div(`class` := "gutterDecoration")(
    Rx {
      val range = (nbLines()._1 until nbLines()._2)
      val topMargin = if (session.getScrollTop() > 0) marginTop := -8 else marginTop := 0
      div(topMargin)(
        for (
          r ← range
        ) yield {
          if (errorsInEditor().exists(_ == r)) {
            errors().find(e ⇒ e._1.line == Some(r)).map { e ⇒
              e._1.line.map { l ⇒
                buildManualPopover(l, e._1.stackTrace, Popup.Left)
              }.getOrElse(buildCleanGutter(r, correctedInEditor()))
            }.getOrElse(buildCleanGutter(r, correctedInEditor()))
          }
          else buildCleanGutter(r, correctedInEditor())
        }
      )
    })

  def setErrors(errorsWithLocation: Seq[ErrorWithLocation]) = {
    nbLines() = (editor.getFirstVisibleRow.toInt, editor.getLastVisibleRow.toInt)
    errors() = errorsWithLocation.map { ewl ⇒ (ewl, ewl.line.map { l ⇒ session.doc.getLine(l) }.getOrElse("")) }
    errorsInEditor() = errors.now.flatMap {
      _._1.line
    }
    correctedInEditor() = Seq()
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

  def apply(fileType: FileExtension, initCode: String, containerModifierSeq: ModifierSeq = emptyMod) = fileType match {
    case OMS   ⇒ editor(initCode, OMS, containerModifierSeq)
    case SCALA ⇒ editor(initCode, SCALA, containerModifierSeq)
    case _     ⇒ empty(initCode, containerModifierSeq)
  }

  def empty(initCode: String, containerModifierSeq: ModifierSeq) = new EditorPanelUI(initCode, NO_EXTENSION, containerModifierSeq)

  private def editor(initCode: String = "", language: FileExtension, containerModifierSeq: ModifierSeq = emptyMod) = new EditorPanelUI(initCode, language, containerModifierSeq)

  def sh(initCode: String = "") = EditorPanelUI(SH, initCode)

}