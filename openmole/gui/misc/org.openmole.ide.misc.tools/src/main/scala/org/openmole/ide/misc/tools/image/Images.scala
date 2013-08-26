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

  def image(path: String): Image = ImageIO.read(classLoader.getResource(path))

  def imageIcon(path: String) = new ImageIcon(classLoader.getResource(path))

  val SPLASH_SCREEN = imageIcon("img/splashscreen.png")

  val START_SLOT = image("img/startSlot.png")
  val START_EXE_SLOT = image("img/startSlotExe.png")
  val INPUT_SLOT = image("img/inputSlot.png")
  val INPUT_EXE_SLOT = image("img/inputSlotExe.png")
  val OUTPUT_SLOT = image("img/outputSlot.png")
  val OUTPUT_EXE_SLOT = image("img/outputSlotExe.png")
  val AGGREGATION_TRANSITION_IMAGE = image("img/aggregation.png")
  val EXPLORATION_TRANSITION_IMAGE = image("img/exploration.png")
  val END_TRANSITION_IMAGE = image("img/endTransition.png")

  val STRAINER_CAPSULE = imageIcon("img/strainerCapsule.png")
  val MASTER_CAPSULE = imageIcon("img/masterCapsule.png")

  val BUILD_EXECUTION = imageIcon("img/build.png")
  val CLEAN_BUILD_EXECUTION = imageIcon("img/cleanAndBuild.png")

  val EYE = imageIcon("img/eye.png")
  val ARROW = imageIcon("img/arrow.png")
  val REFRESH = imageIcon("img/refresh.png")
  val EMPTY = image("img/empty.png")

  val EDIT = imageIcon("img/edit.png")
  val EDIT_EMPTY = imageIcon("img/edit_empty.png")
  val EDIT_ERROR = imageIcon("img/edit_error.png")

  val ADD = imageIcon("img/add.png")
  val DEL = imageIcon("img/del.png")
  val CLOSE = imageIcon("img/close.png")
  val CLOSE_TAB = imageIcon("img/close_tab.png")
  val FILTER = imageIcon("img/filter.png")
  val SETTINGS = imageIcon("img/settings.png")

  val CHECK_VALID = image("img/check_valid.png")
  val CHECK_INVALID = image("img/check_invalid.png")

  val APPLICATION_ICON = imageIcon("img/openmole.png")
  val MOLE_SETTINGS = imageIcon("img/moleSettings.png")

  val HOOK = imageIcon("img/hook.png")
  val SOURCE = imageIcon("img/source.png")
  val SAMPLING_COMPOSITION = imageIcon("img/samplingComposition.png")
  val SAMPLING_COMPOSITION_FAT = imageIcon("img/samplingComposition_fat.png")
}
