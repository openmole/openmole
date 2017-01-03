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

  case class BannerMessage(message: String)

  private val bannerMessages: Var[Seq[BannerMessage]] = Var(Seq())

  val banner = Rx {
    !bannerMessages().isEmpty
  }.expand(
    tags.div(tool.bannerAlert)(
      span(onclick := { () ⇒ close }, tool.bannerAlertClose)(
        raw("&#215")
      ), div(tool.bannerAlertInner)(
        Rx {
          (for {
            bm ← bannerMessages()
          } yield bm.message).map(msg ⇒ div(msg))
        }
      )
    )
  )

  def close = {
    bannerMessages() = Seq()
  }

  def register(bannerMessage: BannerMessage) =
    bannerMessages() = bannerMessages.now :+ bannerMessage

}
