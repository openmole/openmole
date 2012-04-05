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

import org.openide.util.ImageUtilities

object Images {
  
  val classLoader = getClass.getClassLoader
  
  println("----------- " + classLoader.getResource("img/build.png").toString)
  val START_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/startSlot.png").toString)
  val START_EXE_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/startSlotExe.png").toString)
  val INPUT_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/inputSlot.png").toString)
  val INPUT_EXE_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/inputSlotExe.png").toString)
  val OUTPUT_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/outputSlot.png").toString)
  val OUTPUT_EXE_SLOT= ImageUtilities.loadImage(classLoader.getResource("img/outputSlotExe.png").toString)
  val AGGREGATION_TRANSITON = ImageUtilities.loadImage(classLoader.getResource("img/aggregation.png").toString)
  val EXPLORATION_TRANSITON = ImageUtilities.loadImage(classLoader.getResource("img/exploration.png").toString)
  val INPUT_DATA_CHANNEL = ImageUtilities.loadImage(classLoader.getResource("img/inputDataChannel.png").toString)
  val OUTPUT_DATA_CHANNEL = ImageUtilities.loadImage(classLoader.getResource("img/outputDataChannel.png").toString)
  
  val CONNECT_TRANSITION_MODE = ImageUtilities.loadImage(classLoader.getResource("img/connectMode.png").toString)
  val DATA_CHANNEL_TRANSITION_MODE = ImageUtilities.loadImage(classLoader.getResource("img/dataChannelMode.png").toString)
  
  val START_EXECUTION = ImageUtilities.loadImage(classLoader.getResource("img/startExe.png").toString)
  val STOP_EXECUTION = ImageUtilities.loadImage(classLoader.getResource("img/stopExe.png").toString)
  val BUILD_EXECUTION = ImageUtilities.loadImageIcon(classLoader.getResource("img/build.png").toString,true)
  val CLEAN_BUILD_EXECUTION = ImageUtilities.loadImageIcon(classLoader.getResource("img/cleanAndBuild.png").toString,true)
  
  val EYE = ImageUtilities.loadImageIcon(classLoader.getResource("img/eye.png").toString,true)
  val ARROW = ImageUtilities.loadImageIcon(classLoader.getResource("img/arrow.png").toString,true)
  val EMPTY = ImageUtilities.loadImage(classLoader.getResource("img/empty.png").toString)
}