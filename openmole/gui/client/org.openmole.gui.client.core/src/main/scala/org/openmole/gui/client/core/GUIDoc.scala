package org.openmole.gui.client.core

import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.TypedTag
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.all._
import bs._
/*
 * Copyright (C) 27/08/15 // mathieu.leclaire@openmole.org
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

object GUIDoc {

  val doc: TypedTag[HTMLDivElement] = {
    tags.div("Doc !")
  }

}