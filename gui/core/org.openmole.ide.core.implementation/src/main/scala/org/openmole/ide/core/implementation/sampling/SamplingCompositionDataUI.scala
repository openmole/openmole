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
import org.openmole.ide.core.model.sampling._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.data._
import java.awt.Point
import java.security.DomainCombiner
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import collection.mutable.HashMap
import scala.Some

class SamplingCompositionDataUI(val name: String = "",
                                val domains: List[(IDomainProxyUI, Point)] = List.empty,
                                val samplings: List[(ISamplingProxyUI, Point)] = List.empty,
                                val factors: List[IFactorProxyUI] = List.empty,
                                val connections: List[(ISamplingCompositionProxyUI, ISamplingCompositionProxyUI)] = List.empty,
                                val finalSampling: Option[ISamplingProxyUI] = None) extends ISamplingCompositionDataUI {

  val builtSampling = new HashMap[ISamplingProxyUI, Sampling]

  def coreClass = classOf[Sampling]

  def coreObject = {
    builtSampling.clear
    val connectionMap = connections.groupBy {
      _._2
    }
      .map {
        case (k, v) ⇒ k -> v.map {
          _._1
        }
      }

    val samplingMap: Map[String, ISamplingProxyUI] = samplings.map {
      s ⇒ s._1.id -> s._1
    }.toMap
    finalSampling match {
      case Some(fs: ISamplingProxyUI) ⇒ buildSamplingCore(fs, connectionMap, samplingMap)
      case _ ⇒ throw new UserBadDataError("The final sampling is not properly set")
    }
  }

  def buildSamplingCore(proxy: ISamplingProxyUI,
                        connectionMap: Map[ISamplingCompositionProxyUI, List[ISamplingCompositionProxyUI]],
                        samplingMap: Map[String, ISamplingProxyUI]): Sampling = {
    if (!builtSampling.contains(proxy)) {
      val partition = connectionMap.getOrElse(proxy, List()).partition {
        _ match {
          case s: ISamplingProxyUI ⇒ true
          case d: IDomainProxyUI ⇒ false
        }
      }
      val domainsForFactory = domains.filter {
        d ⇒ partition._2.contains(d._1)
      }.map { _._1 }
      builtSampling += proxy -> proxy.dataUI.coreObject(factors.filter {
        f ⇒ domainsForFactory.contains(f.dataUI.domain)
      }.map(_.dataUI),
        partition._1.map {
          p1 ⇒ buildSamplingCore(samplingMap(p1.id), connectionMap, samplingMap)
        })
    }
    builtSampling(proxy)
  }

  def imagePath = "img/samplingComposition.png"

  override def fatImagePath = "img/samplingComposition_fat.png"

  def buildPanelUI = new SamplingCompositionPanelUI(this)
}
