package org.openmole.gui.misc.js

/*import fr.iscpif.scaladget.api.BootstrapTags._
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement }
import scalatags.JsDom.{ tags, TypedTag }

import scalatags.JsDom.all._
import fr.iscpif.scaladget.mapping.tooltipster._
import fr.iscpif.scaladget.mapping.tooltipster.TooltipsterOptions
import fr.iscpif.scaladget.tooltipster._*/

/*
 * Copyright (C) 11/08/15 // mathieu.leclaire@openmole.org
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
object Tooltip {
  /*
  implicit class TypedTagDecorator[T <: HTMLElement](typedTag: TypedTag[T]) {

    def tooltip(
      message:   String,
      direction: Direction    = BottomDirection(),
      level:     TooltipLevel = DefaultTooltipLevel(),
      condition: () ⇒ Boolean = () ⇒ true
    ): HTMLDivElement =
      if (condition()) ToolTipHelp(message, direction, level)(typedTag)
      else tags.div(typedTag).render
  }

}

trait Direction {
  def direction: String
}

case class LeftDirection(direction: String = "left") extends Direction

case class RightDirection(direction: String = "right") extends Direction

case class TopDirection(direction: String = "top") extends Direction

case class BottomDirection(direction: String = "bottom") extends Direction

trait TooltipLevel {
  def level: String
}

case class DefaultTooltipLevel(level: String = "tooltipster-default") extends TooltipLevel

case class WarningTooltipLevel(level: String = "tooltipster-warning") extends TooltipLevel

trait Help {
  def placement: Direction

  def message: String

  def level: TooltipLevel

  def apply[T <: HTMLElement](h: TypedTag[T]): HTMLDivElement = {
    val ttdiv = tags.div(
      title := message
    ).render

    ttdiv.appendChild(h)
    val options = TooltipsterOptions.
      position(placement.direction).
      delay(400).
      theme(level.level)

    toolTip(ttdiv, options)
    ttdiv
  }
}

case class ToolTipHelp(
  message:   String,
  placement: Direction    = BottomDirection(),
  level:     TooltipLevel = DefaultTooltipLevel()
) extends Help

case class NoHelp(
  message:   String       = "",
  placement: Direction    = TopDirection(),
  level:     TooltipLevel = DefaultTooltipLevel()
) extends Help
*/
}