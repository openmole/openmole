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
import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.plugin.sampling.combine.ZipWithNameSampling
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.misc.exception.UserBadDataError
import java.io.File
import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.core.model.data.Prototype
import org.openmole.ide.misc.tools.util.Types
import org.openmole.ide.misc.widget.{ URL, Helper }
import java.util.{ ResourceBundle, Locale }

class ZipWithNameSamplingDataUI(val prototype: Option[IPrototypeDataProxyUI] = None) extends ISamplingDataUI with ZipWithPrototypeSamplingDataUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  def coreObject(factors: List[IFactorDataUI], samplings: List[Sampling]) =
    new ZipWithNameSampling(factors.headOption.getOrElse(throw new UserBadDataError("A factor is required to build a Zip with name Sampling"))
      .coreObject.asInstanceOf[Factor[File, Domain[File] with Discrete[File]]],
      prototype.getOrElse(throw new UserBadDataError("A string prototype is required to build a Zip with name Sampling")).dataUI.coreObject.asInstanceOf[Prototype[String]])

  def buildPanelUI = new ZipWithPrototypeSamplingPanelUI(this) {
    override def help = new Helper(List(new URL(i18n.getString("zipWithNamePermalinkText"), i18n.getString("takePermalink"))))
  }

  def imagePath = "img/zipWithNameSampling.png"

  def fatImagePath = "img/zipWithNameSampling_fat.png"

  def isAcceptable(sampling: ISamplingDataUI) = false

  override def isAcceptable(domain: IDomainDataUI) = domain match {
    case f: IFinite ⇒
      if (Types(domain.domainType.toString, Types.FILE)) true
      else {
        StatusBar().warn("A File domain is required here.")
        false
      }
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Zip with name Sampling")
      false
  }

  def preview = name

  def name = "Zip with " + prototype.getOrElse("").toString + " name"

  def coreClass = classOf[ZipWithNameSampling]
}