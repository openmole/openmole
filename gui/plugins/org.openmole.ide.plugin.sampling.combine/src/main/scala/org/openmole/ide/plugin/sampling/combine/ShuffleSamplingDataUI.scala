/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI, ISamplingDataUI }
import org.openmole.core.model.sampling.Sampling
import org.openmole.plugin.sampling.combine.ShuffleSampling
import org.openmole.misc.exception.UserBadDataError

class ShuffleSamplingDataUI extends ISamplingDataUI {
  def coreObject(factors: List[IFactorDataUI], samplings: List[Sampling]) =
    samplings.headOption match {
      case Some(s: Sampling) ⇒ new ShuffleSampling(s)
      case _ ⇒ throw new UserBadDataError("A Sampling is required as input of a Shuffle Sampling")
    }

  def buildPanelUI = new GenericCombineSamplingPanelUI(this)

  def imagePath = "img/shuffleSampling.png"

  def fatImagePath = "img/shuffleSampling_fat.png"

  def isAcceptable(domain: IDomainDataUI) = false

  def isAcceptable(sampling: ISamplingDataUI) = true

  override def inputNumberConstrainst = Some(1)

  def preview = "Shuffle"

  def coreClass = classOf[ShuffleSampling]
}