/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.sampling

import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.sampling.ISamplingCompositionWidget
import org.openmole.ide.core.model.sampling.ISamplingWidget
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.sampling.ISampling
import org.openmole.ide.core.model.data._
import java.awt.Point

class SamplingCompositionDataUI(val name: String = "",
                                val factors: List[(IFactorDataUI, Point)] = List.empty,
                                val samplings: List[(ISamplingDataUI, Point)] = List.empty,
                                val connections: List[(String, String)] = List.empty,
                                val finalSampling: Option[String] = None) extends ISamplingCompositionDataUI {

  def coreClass = classOf[ISampling]

  def coreObject = samplingWidget(finalSampling) match {
    case Some(data: ISamplingDataUI) ⇒ data.coreObject(factors.map { _._1 },
      samplings.map { _._1 })
    case _ ⇒ throw new UserBadDataError("The final sampling is not properly set")
  }

  def imagePath = "img/samplingComposition.png"

  override def fatImagePath = "img/samplingComposition_fat.png"

  def buildPanelUI = new SamplingCompositionPanelUI(this)

  def samplingWidget(id: Option[String]): Option[ISamplingDataUI] = id match {
    case Some(i: String) ⇒ samplings.map { _._1 }.find { _.id == i }
    case _ ⇒ throw new UserBadDataError("No final sampling is defined ! The Composition sampling can not be built")
  }

}
