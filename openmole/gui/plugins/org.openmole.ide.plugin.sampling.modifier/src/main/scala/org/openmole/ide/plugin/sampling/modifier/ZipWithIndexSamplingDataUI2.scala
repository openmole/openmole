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
package org.openmole.ide.plugin.sampling.modifier

import org.openmole.core.model.sampling.{ Factor, DiscreteFactor, Sampling }
import org.openmole.plugin.sampling.modifier.{ ZipSampling, ZipWithIndexSampling }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.ide.core.implementation.sampling.{ FiniteUI, SamplingUtils }
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class ZipWithIndexSamplingDataUI2(val prototype: Option[PrototypeDataProxyUI] = None)
    extends SamplingDataUI with ZipWithPrototypeSamplingDataUI {

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    ZipWithIndexSampling(SamplingUtils.toUnorderedFactorsAndSamplings(factorOrSampling).headOption.getOrElse(throw new UserBadDataError("A samplingMap is required to build a Zip with index Sampling")),
      prototype.getOrElse(throw new UserBadDataError("A string prototypeMap is required to build a Zip with name Sampling")).dataUI.coreObject.asInstanceOf[Prototype[Int]])

  }
  def buildPanelUI = new ZipWithPrototypeSamplingPanelUI(this) {
    override lazy val help = new Helper(List(new URL(i18n.getString("zipWithIndexPermalinkText"), i18n.getString("zipWithIndexPermalink"))))
  }

  override def imagePath = "img/zipWithIndexSampling.png"

  def fatImagePath = "img/zipWithIndexSampling_fat.png"

  def isAcceptable(sampling: SamplingDataUI) = true

  override def isAcceptable(domain: DomainDataUI) = domain match {
    case f: FiniteUI ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Zip with index Sampling")
      false
  }

  override def inputNumberConstrainst = Some(1)

  def preview = "Zip with " + prototype.getOrElse("").toString + " index"

  def name = "Zip with index"

  def coreClass = classOf[ZipWithIndexSampling]
}