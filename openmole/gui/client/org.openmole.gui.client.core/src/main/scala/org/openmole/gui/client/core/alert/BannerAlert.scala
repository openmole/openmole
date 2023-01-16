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

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{Panels, TextPanel}
import org.openmole.gui.ext.client.*
import org.openmole.gui.ext.client.Utils.*
import scaladget.bootstrapnative.bsn

enum BannerLevel:
  case Regular, Critical

case class BannerMessage(messageDiv: HtmlElement, bannerLevel: BannerLevel)

class BannerAlert:

  val stackPanel: TextPanel = new TextPanel("Error stack")
  private val bannerMessages: Var[Seq[BannerMessage]] = Var(Seq())
  val isOpen = bannerMessages.signal.map { bm ⇒ !bm.isEmpty }

  private val bannerDiv = div(
    child <-- bannerMessages.signal.map { bMessages ⇒
      div(omsheet.bannerAlert, backgroundColor := color,
        div(
          omsheet.bannerAlertInner,
          for {
            bm ← bMessages
          } yield bm.messageDiv
        )
      )
    },
    span(omsheet.closeBanner, onClick --> { _ ⇒ clear },
      "x" //raw("&#215")
    ),
    height := "70")

  def banner(using panels: Panels) = isOpen.expandDiv(bannerDiv, () ⇒ {
    panels.treeNodeTabs.tabsElement.tabs.now().foreach { t ⇒ t.t.resizeEditor }
  })

  def clear = bannerMessages.set(Seq())

  private def registerMessage(bannerMessage: BannerMessage) =
    bannerMessages.update(bm ⇒ (bm :+ bannerMessage).distinct.takeRight(2))

  def registerWithDetails(message: String, details: String, bannerLevel: BannerLevel = BannerLevel.Regular) =
    registerMessage(
      BannerMessage(
        div(span(message), button(bsn.btn_secondary, marginLeft := "10", "Details", onClick --> { _ ⇒
          stackPanel.content.set(details)
          stackPanel.dialog.show
        })),
        BannerLevel.Critical
      )
    )

  def register(message: String, bannerLevel: BannerLevel = BannerLevel.Regular): Unit =
    registerMessage(BannerMessage(div(span(message)), bannerLevel))

  def registerDiv(messageDiv: HtmlElement, level: BannerLevel = BannerLevel.Regular) =
    registerMessage(BannerMessage(messageDiv, level))

  def registerWithStack(message: String, stack: Option[String], bannerLevel: BannerLevel = BannerLevel.Regular) =
    stack match {
      case Some(s) ⇒ registerWithDetails(message, s, bannerLevel)
      case None    ⇒ register(message, bannerLevel)
    }

  private def color = {
    if (bannerMessages.now().exists(_.bannerLevel == BannerLevel.Critical)) omsheet.RED
    else if (bannerMessages.now().isEmpty) omsheet.DARK_GREY
    else omsheet.BLUE
  }

