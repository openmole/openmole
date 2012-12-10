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
import org.openmole.plugin.sampling.combine.ZipWithIndexSampling
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype
import org.openmole.ide.misc.widget.{ URL, Helper }

class ZipWithIndexSamplingDataUI(val prototype: Option[IPrototypeDataProxyUI] = None)
    extends ISamplingDataUI with ZipWithPrototypeSamplingDataUI {

  def coreObject(factors: List[IFactorDataUI], samplings: List[Sampling]) =
    new ZipWithIndexSampling((CombineSamplingCoreFactory(factors) ::: samplings).headOption.getOrElse(throw new UserBadDataError("A sampling is required to build a Zip with index Sampling")),
      prototype.getOrElse(throw new UserBadDataError("A string prototype is required to build a Zip with name Sampling")).dataUI.coreObject.asInstanceOf[Prototype[Int]])

  def buildPanelUI = new ZipWithPrototypeSamplingPanelUI(this) {
    override def help = new Helper(List(new URL(i18n.getString("zipWithIndexPermalinkText"), i18n.getString("takePermalink"))))
  }

  def imagePath = "img/zipWithIndexSampling.png"

  def fatImagePath = "img/zipWithIndexSampling_fat.png"

  def isAcceptable(sampling: ISamplingDataUI) = true

  override def isAcceptable(domain: IDomainDataUI) = domain match {
    case f: IFinite ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Zip with index Sampling")
      false
  }

  override def inputNumberConstrainst = Some(1)

  def preview = name

  def name = "Zip with " + prototype.getOrElse("").toString + " index"

  def coreClass = classOf[ZipWithIndexSampling]
}