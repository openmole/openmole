package org.openmole.gui.client.core.alert

/*
 * Copyright (C) 04/08/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.alert.AbsolutePositioning.*
import org.openmole.gui.client.core.files.{TreeNodeComment, TreeNodeError}
import org.openmole.gui.client.tool.OMTags.*
import org.openmole.gui.ext.data.SafePath
import org.openmole.gui.client.tool.*
import org.openmole.gui.ext.client.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{Panels, TextPanel}
import scaladget.bootstrapnative.bsn.*

class AlertPanel:

  val visible: Var[Boolean] = Var(false)
  val zoneModifier: Var[HESetters] = Var(FullPage.modifierClass)
  val positionModifier: Var[HESetters] = Var(CenterPagePosition.modifierClass)
  val alertElement: Var[HtmlElement] = Var(div())

  val elementDiv =
    div(
      child <-- positionModifier.signal.combineWith(alertElement.signal).map {
        case (pm, ae) ⇒
          div(pm, ae)
      }
    )

  val alertDiv =
    div(
      child <-- visible.signal.combineWith(zoneModifier.signal).map {
        case (v, zm) ⇒
          if (v) div(Seq(alertOverlay, zm)) else div(displayOff)
      },
      elementDiv
    )

  def popup(
    messageDiv:       HtmlElement,
    actions:          Seq[AlertAction],
    position:         Position,
    zone:             Zone             = FullPage,
    alertType:        HESetters        = btn_danger,
    buttonGroupClass: HESetters        = Seq(float := "left"),
    okString:         String           = "OK",
    cancelString:     String           = "Cancel"
  ) = {
    alertElement.set(OMTags.alert(alertType, messageDiv, actions.map { a ⇒ a.copy(action = actionWrapper(a.action)) }, buttonGroupClass, okString, cancelString))
    zoneModifier.set(zone.modifierClass)
    positionModifier.set(position.modifierClass)
    visible.set(true)
  }

  def actionWrapper(action: () ⇒ Unit): () ⇒ Unit = () ⇒ {
    action()
    visible.set(false)
  }

  def alertDiv(
    messageDiv:       HtmlElement,
    okaction:         () ⇒ Unit,
    cancelaction:     () ⇒ Unit   = () ⇒ {},
    transform:        Position    = CenterPagePosition,
    zone:             Zone        = FullPage,
    alertType:        HESetters   = Seq(btn_danger),
    buttonGroupClass: HESetters   = Seq(float := "right"),
    okString:         String      = "OK",
    cancelString:     String      = "Cancel"
  ): Unit = {
    popup(messageDiv, Seq(AlertAction(okaction), AlertAction(cancelaction)), transform, zone, alertType, buttonGroupClass, okString, cancelString)
  }

  def treeNodeErrorDiv(error: TreeNodeError): Unit = alertDiv(
    div(
      error.message,
      OptionsDiv(error.filesInError, SafePath.naming).render
    ), okaction = error.okaction, cancelaction = error.cancelaction, zone = FileZone
  )

  def treeNodeCommentDiv(error: TreeNodeComment): Unit = {
    popup(
      div(
        error.message,
        OptionsDiv(error.filesInError, SafePath.naming).render
      ), Seq(AlertAction(error.okaction)), CenterPagePosition, FileZone, warning, float := "right"
    )
  }

  def string(
    message:          String,
    okaction:         () ⇒ Unit,
    cancelaction:     () ⇒ Unit = () ⇒ {},
    transform:        Position  = CenterPagePosition,
    zone:             Zone      = FullPage,
    alertType:        HESetters = Seq(btn_danger),
    buttonGroupClass: HESetters = Seq(float := "left", marginLeft := "20"),
    okString:         String    = "OK",
    cancelString:     String    = "Cancel"
  ): Unit = alertDiv(
    div(message),
    okaction,
    cancelaction,
    transform,
    zone,
    alertType,
    buttonGroupClass,
    okString,
    cancelString)

  def detail(
    message:          String,
    detail:           String,
    cancelaction:     () ⇒ Unit = () ⇒ {},
    transform:        Position  = CenterPagePosition,
    zone:             Zone      = FullPage,
    alertType:        HESetters = Seq(btn_danger),
    buttonGroupClass: HESetters = Seq(float := "left", marginLeft := "20")
  )(using panels: Panels): Unit =
    alertDiv(
      div(message),
      () ⇒ {
        panels.stackPanel.content.set(detail)
        panels.stackPanel.dialog.show
      }, cancelaction, transform, zone, alertType, buttonGroupClass, "Details"
    )

