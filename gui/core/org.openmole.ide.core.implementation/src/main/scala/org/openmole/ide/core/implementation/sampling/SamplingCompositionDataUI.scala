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
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.data._
import java.awt.Point
import java.security.DomainCombiner
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI

class SamplingCompositionDataUI(val name: String = "",
                                val factors: List[(IFactorDataUI, Point)] = List.empty,
                                val samplings: List[(ISamplingDataUI, Point)] = List.empty,
                                val connections: List[(String, String)] = List.empty,
                                val finalSampling: Option[String] = None) extends ISamplingCompositionDataUI {

  var builtSampling = scala.collection.mutable.Map.empty[ISamplingDataUI, Sampling]
  def coreClass = classOf[Sampling]

  def coreObject = {
    builtSampling = scala.collection.mutable.Map.empty[ISamplingDataUI, Sampling]
    val connectionMap = connections.groupBy { _._2 }.map { case (k, v) ⇒ k -> v.map { _._1 } }
    val factorMap = factors.map { f ⇒ f._1.id -> f._1 }.toMap
    val samplingMap: Map[String, ISamplingDataUI] = samplings.map { s ⇒ s._1.id -> s._1 }.toMap
    finalSampling match {
      case Some(fs: String) ⇒ samplingMap(fs) match {
        case data: ISamplingDataUI ⇒ buildSamplingCore(data, connectionMap, factorMap, samplingMap)
        case _ ⇒ throw new UserBadDataError("ERROR ... ")
      }
      case _ ⇒ throw new UserBadDataError("The final sampling is not properly set")
    }
  }

  def buildSamplingCore(data: ISamplingDataUI,
                        connectionMap: Map[String, List[String]],
                        factorMap: Map[String, IFactorDataUI],
                        samplingMap: Map[String, ISamplingDataUI]): Sampling = {
    println("buildSamplingCore")
    if (!builtSampling.contains(data)) {
      println("build :: " + data.id)
      val partition = connectionMap(data.id).partition { _.substring(0, 4) == "samp" }
      builtSampling += data -> data.coreObject(partition._2.map { factorMap },
        partition._1.map { s ⇒ buildSamplingCore(samplingMap(s), connectionMap, factorMap, samplingMap) })
    }
    builtSampling(data)
  }

  //def connectedFactorsTo(sampling: ISamplingDataUI) = connections.filter { _._2 == sampling.id }.map { s ⇒ samplingDataUI(s._2) }

  def imagePath = "img/samplingComposition.png"

  override def fatImagePath = "img/samplingComposition_fat.png"

  def buildPanelUI = new SamplingCompositionPanelUI(this)
}
