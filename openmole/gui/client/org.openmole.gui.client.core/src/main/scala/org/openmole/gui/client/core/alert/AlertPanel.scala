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

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.core.alert.AbsolutePositioning._
import org.openmole.gui.client.core.files.{ TreeNodeComment, TreeNodeError }
import org.openmole.gui.client.core.panels._
import org.openmole.gui.client.tool.OMTags._
import org.openmole.gui.ext.data.SafePath
import org.openmole.gui.client.tool._
import org.openmole.gui.ext.client._
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLDivElement
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

class AlertPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val visible: Var[Boolean] = Var(false)
  val zoneModifier: Var[ModifierSeq] = Var(FullPage.modifierClass)
  val positionModifier: Var[ModifierSeq] = Var(CenterPagePosition.modifierClass)
  val alertElement: Var[TypedTag[Div]] = Var(tags.div)

  val elementDiv = Rx {
    div(
      positionModifier(),
      alertElement()
    )
  }

  val alertDiv = Rx {
    div(
      if (visible()) alertOverlay +++ zoneModifier() else displayOff
    )(elementDiv)
  }

  def popup(
    messageDiv:       TypedTag[HTMLDivElement],
    actions:          Seq[AlertAction],
    position:         Position,
    zone:             Zone                     = FullPage,
    alertType:        ModifierSeq              = btn_danger,
    buttonGroupClass: ModifierSeq              = floatLeft,
    okString:         String                   = "OK",
    cancelString:     String                   = "Cancel"
  ) = {
    alertElement() = OMTags.alert(alertType, messageDiv, actions.map { a ⇒ a.copy(action = actionWrapper(a.action)) }, buttonGroupClass, okString, cancelString)
    zoneModifier() = zone.modifierClass
    positionModifier() = position.modifierClass
    visible() = true
  }

  def actionWrapper(action: () ⇒ Unit): () ⇒ Unit = () ⇒ {
    action()
    visible() = false
  }

  def alertDiv(
    messageDiv:       TypedTag[HTMLDivElement],
    okaction:         () ⇒ Unit,
    cancelaction:     () ⇒ Unit                = () ⇒ {},
    transform:        Position                 = CenterPagePosition,
    zone:             Zone                     = FullPage,
    alertType:        ModifierSeq              = btn_danger,
    buttonGroupClass: ModifierSeq              = floatRight,
    okString:         String                   = "OK",
    cancelString:     String                   = "Cancel"
  ): Unit = {
    popup(messageDiv, Seq(AlertAction(okaction), AlertAction(cancelaction)), transform, zone, alertType, buttonGroupClass, okString, cancelString)
  }

  def treeNodeErrorDiv(error: TreeNodeError): Unit = alertDiv(
    tags.div(
      error.message,
      OptionsDiv(error.filesInError, SafePath.naming).div
    ), okaction = error.okaction, cancelaction = error.cancelaction, zone = FileZone
  )

  def treeNodeCommentDiv(error: TreeNodeComment): Unit = {
    popup(
      tags.div(
        error.message,
        OptionsDiv(error.filesInError, SafePath.naming).div
      ), Seq(AlertAction(error.okaction)), CenterPagePosition, FileZone, warning, floatRight
    )
  }

  def string(
    message:          String,
    okaction:         () ⇒ Unit,
    cancelaction:     () ⇒ Unit   = () ⇒ {},
    transform:        Position    = CenterPagePosition,
    zone:             Zone        = FullPage,
    alertType:        ModifierSeq = btn_danger,
    buttonGroupClass: ModifierSeq = Seq(floatLeft, marginLeft := 20),
    okString:         String      = "OK",
    cancelString:     String      = "Cancel"
  ): Unit = alertDiv(tags.div(message), okaction, cancelaction, transform, zone, alertType, buttonGroupClass, okString, cancelString)

  def detail(
    message:          String,
    detail:           String,
    cancelaction:     () ⇒ Unit   = () ⇒ {},
    transform:        Position    = CenterPagePosition,
    zone:             Zone        = FullPage,
    alertType:        ModifierSeq = btn_danger,
    buttonGroupClass: ModifierSeq = Seq(floatLeft, marginLeft := 20)
  ): Unit =
    alertDiv(
      tags.div(message),
      () ⇒ {
        stackPanel.content() = detail
        stackPanel.dialog.show
      }, cancelaction, transform, zone, alertType, buttonGroupClass, "Details"
    )
}
