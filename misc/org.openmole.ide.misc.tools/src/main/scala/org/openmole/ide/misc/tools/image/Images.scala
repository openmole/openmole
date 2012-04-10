/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
  
  val START_SLOT= ImageIO.read(classLoader.getResource("img/startSlot.png")).asInstanceOf[Image]
  val START_EXE_SLOT= ImageIO.read(classLoader.getResource("img/startSlotExe.png")).asInstanceOf[Image]
  val INPUT_SLOT= ImageIO.read(classLoader.getResource("img/inputSlot.png")).asInstanceOf[Image]
  val INPUT_EXE_SLOT= ImageIO.read(classLoader.getResource("img/inputSlotExe.png")).asInstanceOf[Image]
  val OUTPUT_SLOT= ImageIO.read(classLoader.getResource("img/outputSlot.png")).asInstanceOf[Image]
  val OUTPUT_EXE_SLOT= ImageIO.read(classLoader.getResource("img/outputSlotExe.png")).asInstanceOf[Image]
  val AGGREGATION_TRANSITON = ImageIO.read(classLoader.getResource("img/aggregation.png")).asInstanceOf[Image]
  val EXPLORATION_TRANSITON = ImageIO.read(classLoader.getResource("img/exploration.png")).asInstanceOf[Image]
  val INPUT_DATA_CHANNEL = ImageIO.read(classLoader.getResource("img/inputDataChannel.png")).asInstanceOf[Image]
  val OUTPUT_DATA_CHANNEL = ImageIO.read(classLoader.getResource("img/outputDataChannel.png")).asInstanceOf[Image]
  
  val CONNECT_TRANSITION_MODE = new ImageIcon(classLoader.getResource("img/connectMode.png"))
  val DATA_CHANNEL_TRANSITION_MODE = new ImageIcon(classLoader.getResource("img/dataChannelMode.png"))
  
  val START_EXECUTION = ImageIO.read(classLoader.getResource("img/startExe.png")).asInstanceOf[Image]
  val STOP_EXECUTION = ImageIO.read(classLoader.getResource("img/stopExe.png")).asInstanceOf[Image]
  val BUILD_EXECUTION = new ImageIcon(classLoader.getResource("img/build.png"))
  val CLEAN_BUILD_EXECUTION = new ImageIcon(classLoader.getResource("img/cleanAndBuild.png"))
  
  val EYE = new ImageIcon(classLoader.getResource("img/eye.png"))
  val ARROW = new ImageIcon(classLoader.getResource("img/arrow.png"))
  val REFRESH = new ImageIcon(classLoader.getResource("img/refresh.png"))
  val EMPTY = ImageIO.read(classLoader.getResource("img/empty.png")).asInstanceOf[Image]
  
  val EDIT = new ImageIcon(classLoader.getResource("img/edit.png"))
  val EDIT_EMPTY = new ImageIcon(classLoader.getResource("img/edit_empty.png"))
  val EDIT_ERROR = new ImageIcon(classLoader.getResource("img/edit_error.png"))
  
  val ADD = new ImageIcon(classLoader.getResource("img/add.png"))
  val DEL = new ImageIcon(classLoader.getResource("img/del.png"))
  val NEXT = new ImageIcon(classLoader.getResource("img/next.png"))
  val PREVIOUS = new ImageIcon(classLoader.getResource("img/previous.png"))
  val CLOSE = new ImageIcon(classLoader.getResource("img/close.png"))
  val CLOSE_TAB = new ImageIcon(classLoader.getResource("img/close_tab.png"))
  
  
}