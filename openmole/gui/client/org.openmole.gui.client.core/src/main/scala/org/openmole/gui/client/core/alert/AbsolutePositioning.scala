package org.openmole.gui.client.core.alert

import org.openmole.gui.client.ext._

import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

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

object AbsolutePositioning {

  trait LayoutModifier {
    def modifierClass: HESetters
  }

  trait Position extends LayoutModifier

  object CenterPagePosition extends Position {
    lazy val modifierClass = omsheet.centerPage("150px")
  }

  object RelativeCenterPosition extends Position {
    lazy val modifierClass = omsheet.relativeCenter
  }

  object RightPosition extends Position {
    lazy val modifierClass = omsheet.rightPage
  }

  case class XYTransform(x: Int, y: Int) extends Position {
    lazy val modifierClass = Seq(
      left := x.toString,
      top := y.toString
    )
  }

  trait Zone extends LayoutModifier

  object FullPage extends Zone {
    lazy val modifierClass = omsheet.fullPageZone
  }

  object FileZone extends Zone {
    lazy val modifierClass = omsheet.fileZone
  }

  object TopZone extends Zone {
    lazy val modifierClass = omsheet.topZone
  }

}
