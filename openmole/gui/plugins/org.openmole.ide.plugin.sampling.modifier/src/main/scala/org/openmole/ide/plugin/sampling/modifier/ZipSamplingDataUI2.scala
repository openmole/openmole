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

import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.plugin.sampling.modifier.ZipSampling
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.ide.core.implementation.sampling.{ SamplingUtils }
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }
import org.openmole.ide.core.implementation.panel.NoParameterSamplingPanelUI
import java.util.{ Locale, ResourceBundle }

class ZipSamplingDataUI2 extends SamplingDataUI {
  def name = "Zip"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    ZipSampling(SamplingUtils.toUnorderedFactorsAndSamplings(factorOrSampling): _*)
  }

  def buildPanelUI = new NoParameterSamplingPanelUI(this) {
    val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))
    override lazy val help = new Helper(List(new URL(i18n.getString("zipPermalinkText"),
      i18n.getString("zipPermalink"))))
  }

  override def imagePath = "img/zipSampling.png"

  def fatImagePath = "img/zipSampling_fat.png"

  def isAcceptable(sampling: SamplingDataUI) = true

  override def isAcceptable(domain: DomainDataUI) = true

  def preview = name

  def coreClass = classOf[ZipSampling]
}