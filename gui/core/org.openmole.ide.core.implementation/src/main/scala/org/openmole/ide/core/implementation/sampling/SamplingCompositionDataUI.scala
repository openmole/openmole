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
                                val connections: List[(ISamplingCompositionProxyUI, ISamplingCompositionProxyUI)] = List.empty,
                                val finalSampling: Option[ISamplingProxyUI] = None) extends ISamplingCompositionDataUI {

  val builtSampling = new HashMap[ISamplingProxyUI, Sampling]

  def coreClass = classOf[Sampling]

  def coreObject = {
    builtSampling.clear
    val connectionMap = connections.groupBy {
      _._2
    }.map {
      case (k, v) ⇒ k -> v.map {
        _._1
      }
    }
    val domainMap: Map[String, IDomainProxyUI] = domains.map {
      f ⇒ f._1.id -> f._1
    }.toMap
    val samplingMap: Map[String, ISamplingProxyUI] = samplings.map {
      s ⇒ s._1.id -> s._1
    }.toMap
    finalSampling match {
      case Some(fs: ISamplingProxyUI) ⇒ buildSamplingCore(fs, connectionMap, domainMap, samplingMap)
      case _ ⇒ throw new UserBadDataError("The final sampling is not properly set")
    }
  }

  def buildSamplingCore(widget: ISamplingProxyUI,
                        connectionMap: Map[ISamplingCompositionProxyUI, List[ISamplingCompositionProxyUI]],
                        domainMap: Map[String, IDomainProxyUI],
                        samplingMap: Map[String, ISamplingProxyUI]): Sampling = {
    if (!builtSampling.contains(widget)) {
      val partition = connectionMap.getOrElse(widget, List()).partition {
        _ match {
          case s: ISamplingWidget ⇒ true
          case _ ⇒ false
        }
      }
      //_.substring(0, 4) == "samp" }
      //FIXME : add prototypes and build factors
      //  builtSampling += data -> data.coreObject(partition._2.map { domainMap },
      //   partition._1.filterNot(_ == data.id).map { s ⇒ buildSamplingCore(samplingMap(s), connectionMap, domainMap, samplingMap) })
    }
    builtSampling(widget)
  }

  def imagePath = "img/samplingComposition.png"

  override def fatImagePath = "img/samplingComposition_fat.png"

  def buildPanelUI = new SamplingCompositionPanelUI(this)
}
