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

package org.openmole.ide.core.implementation.workflow

import org.openide.util.ImageUtilities

object Images {
  
  val IMAGE_START_SLOT= ImageUtilities.loadImage("img/startSlot.png")
  val IMAGE_START_EXE_SLOT= ImageUtilities.loadImage("img/startSlotExe.png")
  val IMAGE_INPUT_SLOT= ImageUtilities.loadImage("img/inputSlot.png")
  val IMAGE_INPUT_EXE_SLOT= ImageUtilities.loadImage("img/inputSlotExe.png")
  val IMAGE_OUTPUT_SLOT= ImageUtilities.loadImage("img/outputSlot.png")
  val IMAGE_OUTPUT_EXE_SLOT= ImageUtilities.loadImage("img/outputSlotExe.png")
  val AGGREGATION_TRANSITON = ImageUtilities.loadImage("img/aggregation.png")
  val EXPLORATION_TRANSITON = ImageUtilities.loadImage("img/exploration.png")
  val IMAGE_INPUT_DATA_CHANNEL = ImageUtilities.loadImage("img/inputDataChannel.png")
  val IMAGE_OUTPUT_DATA_CHANNEL = ImageUtilities.loadImage("img/outputDataChannel.png")
  val THUMB_SIZE = 24
}