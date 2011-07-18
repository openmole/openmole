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

import java.awt.Image
import java.awt.Toolkit
import java.awt.image.FilteredImageSource
import java.awt.image.ReplicateScaleFilter
import org.openide.util.ImageUtilities
import scala.collection.mutable.HashMap

object Images {
  
  val IMAGE_START_SLOT= ImageUtilities.loadImage("img/startSlot.png")
  val IMAGE_INPUT_SLOT= ImageUtilities.loadImage("img/inputSlot.png")
  val IMAGE_OUTPUT_SLOT= ImageUtilities.loadImage("img/outputSlot.png")
  val AGGREGATION_TRANSITON = ImageUtilities.loadImage("img/aggregation.png")
  val EXPLORATION_TRANSITON = ImageUtilities.loadImage("img/exploration.png")
  val THUMB_SIZE = 24

  var thumbPaths = new HashMap[String,Image]
  
  def thumb(path: String, size: Int): Image =  thumbPaths.getOrElseUpdate(path, Toolkit.getDefaultToolkit.createImage(new FilteredImageSource(ImageUtilities.loadImage(path).getSource,new ReplicateScaleFilter(size,size))))
  def thumb(path: String): Image = thumb(path, THUMB_SIZE)
}