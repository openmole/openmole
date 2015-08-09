package org.openmole.gui.client.core

import org.scalajs.dom.html.Div

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

  trait Transform {
    def apply(d: Div) = {
      d.style.position = "absolute"
      transform(d)
    }

    protected def transform: Div ⇒ Div
  }

  case class CustomTransform(d: Div, t: Div ⇒ Div) extends Transform {
    def transform = t
  }

  case class CenterTransform() extends Transform {
    def transform = (d: Div) ⇒ {
      d.className = "centerPage"
      d
    }
  }

  case class RightTransform() extends Transform {
    def transform = (d: Div) ⇒ {
      d.className = "rightPage"
      d
    }
  }

  case class XYTransform(x: Int, y: Int) extends Transform {
    def transform = (d: Div) ⇒ {
      d.style.left = x.toString
      d.style.top = y.toString
      d
    }
  }

  trait Zone {
    def zoneClass: String
  }

  case class FullPage() extends Zone {
    def zoneClass = "fullPageZone"
  }

  case class FileZone() extends Zone {
    def zoneClass = "fileZone"
  }

  case class TopZone() extends Zone {
    def zoneClass = "topZone"
  }

}
