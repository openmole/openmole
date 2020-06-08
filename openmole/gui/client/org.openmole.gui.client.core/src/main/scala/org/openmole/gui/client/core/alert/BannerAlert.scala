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

object BannerAlert {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  sealed trait BannerLevel

  object RegularBannerLevel extends BannerLevel

  object CriticalBannerLevel extends BannerLevel

  def message(message: String, bannerLevel: BannerLevel = RegularBannerLevel) = div(tags.div(message), bannerLevel)

  def div(messageDiv: TypedTag[HTMLDivElement], bannerLevel: BannerLevel = RegularBannerLevel) = BannerMessage(messageDiv, bannerLevel)

  case class BannerMessage(messageDiv: TypedTag[HTMLDivElement], bannerLevel: BannerLevel) {
    def critical = copy(bannerLevel = CriticalBannerLevel)
  }

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
    org.openmole.gui.client.core.panels.treeNodeTabs.tabs.now.foreach { t ⇒
      t.resizeEditor
    }
  })

  def clear = {
    bannerMessages() = Seq()
  }

  def register(bannerMessage: BannerMessage) =
    bannerMessages() = (bannerMessages.now :+ bannerMessage).distinct.takeRight(2)

  def registerWithDetails(message: String, details: String) =
    BannerAlert.register(BannerMessage(tags.div(tags.span(message), tags.button(btn_default +++ (marginLeft := 10), "Details", onclick := { () ⇒
      stackPanel.content() = details
      stackPanel.dialog.show
    })), CriticalBannerLevel))

  private def color = {
    if (bannerMessages.now.exists(_.bannerLevel == CriticalBannerLevel)) omsheet.RED
    else if (bannerMessages.now.isEmpty) omsheet.DARK_GREY
    else omsheet.BLUE
  }

}
