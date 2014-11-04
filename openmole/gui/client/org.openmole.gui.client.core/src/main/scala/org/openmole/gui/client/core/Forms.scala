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

  def levelComponent(prefix: ClassKeyAggregator, level: String) = prefix + (prefix.key + "-" + level)
  def defaultComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "default")
  def primaryComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "primary")
  def successComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "success")
  def infoComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "info")
  def warningComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "warning")
  def dangerComponent(prefix: ClassKeyAggregator) = levelComponent(prefix, "danger")

  def label(keys: ClassKeyAggregator)(content: String) = span(`class` := keys.key)(content)
  private val labelPrefix = key("label")
  val label_default = defaultComponent(labelPrefix)
  val label_primary = primaryComponent(labelPrefix)
  val label_success = successComponent(labelPrefix)
  val label_info = infoComponent(labelPrefix)
  val label_warning = warningComponent(labelPrefix)
  val label_danger = dangerComponent(labelPrefix)

  def btn(keys: ClassKeyAggregator)(content: String) = button(`class` := keys.key)(content)
  private val btnPrefix = key("btn")
  val btn_default = defaultComponent(btnPrefix)
  val btn_primary = primaryComponent(btnPrefix)
  val btn_success = successComponent(btnPrefix)
  val btn_info = infoComponent(btnPrefix)
  val btn_warning = warningComponent(btnPrefix)
  val btn_danger = dangerComponent(btnPrefix)

  def btnGroup(btns: (ClassKeyAggregator, String)*) = {
    val btnDiv = div(`class` := "btn-group")
    btnDiv.apply(btns.map {
      case (key, content) ⇒
        btn(key)(content)
    })
  }

}