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
import org.openmole.core.model.sampling.{ Factor, DiscreteFactor, Sampling }
import org.openmole.plugin.sampling.combine.TakeSampling
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.ide.core.implementation.sampling.SamplingUtils

class TakeSamplingDataUI(val size: String = "1") extends ISamplingDataUI {

  val name = "Take"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) =
    new TakeSampling(SamplingUtils.toUnorderedFactorsAndSamplings(factorOrSampling).head, size.toInt)

  def buildPanelUI = new TakeSamplingPanelUI(this)

  def imagePath = "img/takeSampling.png"

  def fatImagePath = "img/takeSampling_fat.png"

  override def isAcceptable(domain: IDomainDataUI) = domain match {
    case f: IFinite ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Take Sampling")
      false
  }

  def isAcceptable(sampling: ISamplingDataUI) = true

  override def inputNumberConstrainst = Some(1)

  def preview = "Take (" + size + ")"

  def coreClass = classOf[TakeSampling]
}