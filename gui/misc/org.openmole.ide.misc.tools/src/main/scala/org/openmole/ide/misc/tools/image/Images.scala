/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.misc.tools.image

import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.ImageIcon

object Images {

  val classLoader = getClass.getClassLoader

  val START_SLOT: Image = ImageIO.read(classLoader.getResource("img/startSlot.png"))
  val START_EXE_SLOT: Image = ImageIO.read(classLoader.getResource("img/startSlotExe.png"))
  val INPUT_SLOT: Image = ImageIO.read(classLoader.getResource("img/inputSlot.png"))
  val INPUT_EXE_SLOT: Image = ImageIO.read(classLoader.getResource("img/inputSlotExe.png"))
  val OUTPUT_SLOT: Image = ImageIO.read(classLoader.getResource("img/outputSlot.png"))
  val OUTPUT_EXE_SLOT: Image = ImageIO.read(classLoader.getResource("img/outputSlotExe.png"))
  val AGGREGATION_TRANSITION_IMAGE: Image = ImageIO.read(classLoader.getResource("img/aggregation.png"))
  val EXPLORATION_TRANSITION_IMAGE: Image = ImageIO.read(classLoader.getResource("img/exploration.png"))
  val END_TRANSITION_IMAGE: Image = ImageIO.read(classLoader.getResource("img/endTransition.png"))

  val BUILD_EXECUTION = new ImageIcon(classLoader.getResource("img/build.png"))
  val CLEAN_BUILD_EXECUTION = new ImageIcon(classLoader.getResource("img/cleanAndBuild.png"))

  val EYE = new ImageIcon(classLoader.getResource("img/eye.png"))
  val ARROW = new ImageIcon(classLoader.getResource("img/arrow.png"))
  val REFRESH = new ImageIcon(classLoader.getResource("img/refresh.png"))
  val EMPTY: Image = ImageIO.read(classLoader.getResource("img/empty.png"))

  val EDIT = new ImageIcon(classLoader.getResource("img/edit.png"))
  val EDIT_EMPTY = new ImageIcon(classLoader.getResource("img/edit_empty.png"))
  val EDIT_ERROR = new ImageIcon(classLoader.getResource("img/edit_error.png"))

  val ADD = new ImageIcon(classLoader.getResource("img/add.png"))
  val DEL = new ImageIcon(classLoader.getResource("img/del.png"))
  val NEXT = new ImageIcon(classLoader.getResource("img/next.png"))
  val PREVIOUS = new ImageIcon(classLoader.getResource("img/previous.png"))
  val CLOSE = new ImageIcon(classLoader.getResource("img/close.png"))
  val CLOSE_TAB = new ImageIcon(classLoader.getResource("img/close_tab.png"))

  val CHECK_VALID: Image = ImageIO.read(classLoader.getResource("img/check_valid.png"))
  val CHECK_INVALID: Image = ImageIO.read(classLoader.getResource("img/check_invalid.png"))

  val APPLICATION_ICON = new ImageIcon(classLoader.getResource("img/openmole.png"))
  val MOLE_SETTINGS = new ImageIcon(classLoader.getResource("img/moleSettings.png"))
}
