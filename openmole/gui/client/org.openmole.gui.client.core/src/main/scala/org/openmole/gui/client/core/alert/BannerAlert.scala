package org.openmole.gui.client.core.alert

/*
 * Copyright (C) 30/12/16 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.files.TreeNodeTabs
import org.openmole.gui.client.core.panels.stackPanel
import rx._
import scalatags.JsDom.all._
import scaladget.tools._
import scalatags.JsDom.all.{ onclick, raw, span }
import org.openmole.gui.ext.client._
import org.openmole.gui.ext.client.Utils._
import org.scalajs.dom.raw.HTMLDivElement
import scaladget.bootstrapnative.bsn.btn_default
import scalatags.JsDom.{ TypedTag, tags }
import org.openmole.gui.ext.data._

object BannerLevel {
  object Regular extends BannerLevel
  object Critical extends BannerLevel
}

sealed trait BannerLevel

case class BannerMessage(messageDiv: TypedTag[HTMLDivElement], bannerLevel: BannerLevel)

class BannerAlert(resizeTabs: () ⇒ Unit) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  private val bannerMessages: Var[Seq[BannerMessage]] = Var(Seq())
  val isOpen = bannerMessages.map { bm ⇒ !bm.isEmpty }

  private val bannerDiv = tags.div(
    Rx {
      tags.div(omsheet.bannerAlert +++ (backgroundColor := color))(
        tags.div(omsheet.bannerAlertInner)(
          for {
            bm ← bannerMessages()
          } yield bm.messageDiv
        )
      )
    }, span(omsheet.closeBanner, onclick := { () ⇒ clear })(
      raw("&#215")
    )
  )(height := 70)

  lazy val banner = isOpen.expandDiv(bannerDiv, () ⇒ {
    resizeTabs()
  })

  def clear = bannerMessages() = Seq()

  private def registerMessage(bannerMessage: BannerMessage) =
    bannerMessages() = (bannerMessages.now :+ bannerMessage).distinct.takeRight(2)

  def registerWithDetails(message: String, details: String) =
    registerMessage(
      BannerMessage(
        tags.div(tags.span(message), tags.button(btn_default +++ (marginLeft := 10), "Details", onclick := { () ⇒
          stackPanel.content() = details
          stackPanel.dialog.show
        })),
        BannerLevel.Critical
      )
    )

  def register(message: String, bannerLevel: BannerLevel = BannerLevel.Regular): Unit =
    registerMessage(BannerMessage(tags.div(tags.span(message)), bannerLevel))

  def registerDiv(messageDiv: TypedTag[HTMLDivElement], level: BannerLevel = BannerLevel.Regular) =
    registerMessage(BannerMessage(messageDiv, level))

  def registerWithStack(message: String, stack: Option[String]) =
    stack match {
      case Some(s) ⇒ registerWithDetails(message, s)
      case None    ⇒ register(message)
    }

  private def color = {
    if (bannerMessages.now.exists(_.bannerLevel == BannerLevel.Critical)) omsheet.RED
    else if (bannerMessages.now.isEmpty) omsheet.DARK_GREY
    else omsheet.BLUE
  }

}
