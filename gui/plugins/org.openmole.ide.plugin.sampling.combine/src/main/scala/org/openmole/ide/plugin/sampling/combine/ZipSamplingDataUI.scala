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

import org.openmole.ide.core.model.data.{ IDomainDataUI, IFactorDataUI, ISamplingDataUI }
import org.openmole.core.model.sampling.Sampling
import org.openmole.plugin.sampling.combine.ZipSampling
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.ide.core.implementation.dialog.StatusBar

class ZipSamplingDataUI extends ISamplingDataUI {
  def name = "Zip"

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) =
    new ZipSampling((CombineSamplingCoreFactory(factors) ::: samplings): _*)

  def buildPanelUI = new GenericCombineSamplingPanelUI(this) {
    override val help = new Helper(List(new URL(i18n.getString("zipPermalinkText"),
      i18n.getString("zipPermalink"))))
  }

  def imagePath = "img/zipSampling.png"

  def fatImagePath = "img/zipSampling_fat.png"

  def isAcceptable(sampling: ISamplingDataUI) = true

  override def isAcceptable(domain: IDomainDataUI) = true

  def preview = name

  def coreClass = classOf[ZipSampling]
}