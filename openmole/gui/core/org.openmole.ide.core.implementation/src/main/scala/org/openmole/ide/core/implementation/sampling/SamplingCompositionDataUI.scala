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
import org.openmole.core.model.sampling.{ Factor, DiscreteFactor, Sampling }
import java.awt.Point
import scala.util.Try
import org.openmole.core.model.domain.{ Discrete, Domain }
import scala.Some
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.data.{ ImageView, DataUI, IFactorDataUI }
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.panel.Settings

class SamplingCompositionDataUI(val name: String = "",
                                val domains: List[(DomainProxyUI, Point)] = List.empty,
                                val samplings: List[(SamplingProxyUI, Point)] = List.empty,
                                val factors: List[IFactorProxyUI] = List.empty,
                                val connections: List[(SamplingOrDomainProxyUI, SamplingOrDomainProxyUI)] = List.empty,
                                val finalSampling: Option[SamplingOrDomainProxyUI] = None,
                                val finalPosition: (Int, Int) = (450, 200)) extends DataUI with ImageView { samplingCompositionDataUI ⇒

  type T = Domain[Any] with Discrete[Any]

  override def toString: String = name
  //val builtSampling = new HashMap[SamplingOrDomainProxyUI, Sampling]

  def coreClass = classOf[Sampling]

  def coreObject = Try {
    val connectionMap = connections.groupBy {
      _._2
    }
      .map {
        case (k, v) ⇒ k -> v.map {
          _._1
        }
      }

    val samplingMap: Map[String, (SamplingProxyUI, Int)] = samplings.map {
      s ⇒ s._1.id -> (s._1, s._1.ordering)
    }.toMap

    finalSampling match {
      case Some(fs: SamplingOrDomainProxyUI) ⇒ buildSamplingCore(fs, connectionMap, samplingMap)
      case _                                 ⇒ throw new UserBadDataError("The final Sampling is not properly set")
    }
  }

  def buildSamplingCore(proxy: SamplingOrDomainProxyUI,
                        connectionMap: Map[SamplingOrDomainProxyUI, List[SamplingOrDomainProxyUI]],
                        samplingMap: Map[String, (SamplingProxyUI, Int)]): Sampling = {
    val partition = connectionMap.getOrElse(proxy, List()).partition {
      _ match {
        case s: SamplingProxyUI ⇒ true
        case d: DomainProxyUI   ⇒ false
      }
    }

    val domainsForFactory = domains.filter {
      d ⇒ partition._2.contains(d._1)
    }.map {
      _._1
    }

    def coreObject(proxy: SamplingOrDomainProxyUI): Sampling = {
      proxy match {
        case s: SamplingProxyUI ⇒ s.dataUI.coreObject(factors.filter {
          f ⇒ domainsForFactory.contains(f.dataUI.domain)
        }.map(d ⇒ Left(toFactor(d.dataUI), d.dataUI.domain.ordering)) :::
          partition._1.filterNot {
            _.id == proxy.id
          }.map {
            p1 ⇒
              Right(buildSamplingCore(samplingMap(p1.id)._1, connectionMap, samplingMap), samplingMap(p1.id)._2)
          }).get
        case d: DomainProxyUI ⇒
          factors.find {
            _.dataUI.domain.id == d.id
          } match {
            case Some(factor: IFactorProxyUI) ⇒
              factor.dataUI.domain.dataUI.coreObject.get match {
                case dd: T ⇒
                  DiscreteFactor(factor.dataUI.coreObject.get.asInstanceOf[Factor[Any, Domain[Any] with Discrete[Any]]])
                case _ ⇒ throw new UserBadDataError("Only Discrete Domain can be set as final Domain")
              }
            case _ ⇒ throw new UserBadDataError("The final Domain is not properly set")
          }
        case _ ⇒ throw new UserBadDataError("The final Sampling is not properly set")
      }
    }

    def toFactor(f: IFactorDataUI) =
      f.prototype match {
        case Some(p: PrototypeDataProxyUI) ⇒
          Factor(p.dataUI.coreObject.asInstanceOf[Prototype[Any]],
            f.domain.dataUI.coreObject.asInstanceOf[Domain[Any]])
        case _ ⇒ throw new UserBadDataError("No Prototype is define for the domain " + f.domain.dataUI.preview)
      }

    coreObject(proxy)
  }

  override def imagePath = "img/samplingComposition.png"

  def fatImagePath = "img/samplingComposition_fat.png"

  def buildPanelUI: Settings = new SamplingCompositionPanelUI {
    //type DATAUI = samplingCompositionDataUI.type
    val dataUI = samplingCompositionDataUI
  }

  def cloneWithoutPrototype(proxy: PrototypeDataProxyUI): SamplingCompositionDataUI = this
}
