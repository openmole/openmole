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

import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.plugin.sampling.combine.TakeSampling
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.sampling.{ FiniteUI, SamplingUtils }
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }

class TakeSamplingDataUI(val size: String = "1") extends SamplingDataUI {

  val name = "Take"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    new TakeSampling(SamplingUtils.toUnorderedFactorsAndSamplings(factorOrSampling).head, size.toInt)
  }

  def buildPanelUI = new TakeSamplingPanelUI(this)

  override def imagePath = "img/takeSampling.png"

  def fatImagePath = "img/takeSampling_fat.png"

  override def isAcceptable(domain: DomainDataUI) = domain match {
    case f: FiniteUI ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Take Sampling")
      false
  }

  def isAcceptable(sampling: SamplingDataUI) = true

  override def inputNumberConstrainst = Some(1)

  def preview = "Take (" + size + ")"

  def coreClass = classOf[TakeSampling]
}