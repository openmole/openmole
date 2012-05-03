/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.dialog

import java.awt.Color
import scala.swing.Label

object StatusBar extends Label {
  background = Color.WHITE
  opaque = true

  def inform(info: String) = text = info

  def warn(warning: String) = text = "<html><p style=\"color:#FF9955\">" + warning + "</p></html>"

  def block(warning: String) = text = "<html><p style=\"color:#DD0000\">" + warning + "</p></html>"
}
