package org.openmole.gui.client.core

/*
 * Copyright (C) 03/11/14 // mathieu.leclaire@openmole.org
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

import scalatags.JsDom._
import all._

object Forms {

  implicit def classKeyAggregatorToString(ck: ClassKeyAggregator): String = ck.key

  def key(s: String) = new ClassKeyAggregator(s)

  // def navbar(keys: ClassKeyAggregator) = nav(`class` := keys.key)
  private val navPrefix = key("navbar")
  val nav_default = navPrefix + "navbar-default"
  val nav_inverse = navPrefix + "navbar-inverse"
  val nav_staticTop = "navbar-static-top"

  def label(keys: ClassKeyAggregator)(content: String) = span(`class` := keys.key)(content)
  private val labelPrefix = key("label")
  val label_default = labelPrefix + "label-default"
  val label_primary = labelPrefix + "label-primary"
  val label_success = labelPrefix + "label-success"
  val label_info = labelPrefix + "label-info"
  val label_warning = labelPrefix + "label-warning"
  val label_danger = labelPrefix + "label-danger"

}