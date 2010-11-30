/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.control

class QualityControl extends IQualityControl {

  @volatile var _failureRate = 0
  //@volatile var _quality = 1

  override def failed = _failureRate += 1
  override def success = _failureRate -= 1
  override def failureRate: Int = _failureRate
  override def reinit = _failureRate = 0
    
//  override def increaseQuality(value: Int) = synchronized {
//    _quality += value
//    val max = Activator.getWorkspace.preferenceAsInt(QualityControl.MaxQuality)
//    if(_quality > max) _quality = max
//  }
//  
//  override def decreaseQuality(value: Int) = synchronized {
//    _quality -= value
//    if(_quality < 1) _quality = 1
//  }
//  
//  override def quality: Int = _quality
}
