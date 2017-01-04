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

import rx._

import scalatags.JsDom.all._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._

import scalatags.JsDom.all.{ onclick, raw, span }
import org.openmole.gui.client.tool.JsRxTags._
import fr.iscpif.scaladget.api.BootstrapTags._
import org.openmole.gui.client.tool

import scalatags.JsDom.tags

object BannerAlert {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  sealed trait BannerLevel

  object RegularBannerLevel extends BannerLevel

  object CriticalBannerLevel extends BannerLevel

  case class BannerMessage(message: String, bannerLevel: BannerLevel = RegularBannerLevel) {
    def critical = copy(bannerLevel = CriticalBannerLevel)
  }

  private val bannerMessages: Var[Seq[BannerMessage]] = Var(Seq())

  private val bannerDiv = div(Rx {
    tags.div(tool.bannerAlert +++ (backgroundColor := color))(
      span(onclick := { () ⇒ close }, tool.bannerAlertClose)(
        raw("&#215")
      ), div(tool.bannerAlertInner)(
        (for {
          bm ← bannerMessages()
        } yield bm.message).map(msg ⇒ div(msg))
      )
    )
  })

  val banner = Rx {
    !bannerMessages().isEmpty
  }.expand(bannerDiv)

  def close = {
    bannerMessages() = Seq()
  }

  def register(bannerMessage: BannerMessage) =
    bannerMessages() = (bannerMessages.now :+ bannerMessage).distinct

  private def color = {
    if (bannerMessages.now.exists(_.bannerLevel == CriticalBannerLevel)) tool.RED
    else if (bannerMessages.now.isEmpty) tool.DARK_GREY
    else tool.BLUE
  }

}
