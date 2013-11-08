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

package org.openmole.ide.plugin.sampling.modifier

import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.implementation.builder._
import org.openmole.core.model.sampling.Sampling
import org.openmole.plugin.sampling.modifier._
import org.openmole.ide.core.implementation.sampling.{ IBuiltCompositionSampling, SamplingProxyUI }
import org.openmole.ide.core.implementation.factory.SamplingFactoryUI

class Activator extends OSGiActivator with SamplingActivator {
  override def samplingFactories = List(
    new SamplingFactoryUI {
      def buildDataUI = new ShuffleSamplingDataUI010

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: ShuffleSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.sampling), bSC))
          case _                   ⇒ (proxy, bSC)
        }
      }
    }, new SamplingFactoryUI {
      def buildDataUI = new TakeSamplingDataUI010

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        sampling match {
          case cs: TakeSampling ⇒
            val proxy = SamplingProxyUI(new TakeSamplingDataUI010(cs.n.toString))
            (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.sampling), bSC))
          case _ ⇒ (SamplingProxyUI(buildDataUI), bSC)
        }
      }
    }, new SamplingFactoryUI {
      def buildDataUI = new ZipSamplingDataUI010

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: ZipSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, cs.samplings, bSC))
          case _               ⇒ (proxy, bSC)
        }
      }
    }, new SamplingFactoryUI {
      def buildDataUI = new ZipWithIndexSamplingDataUI010

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) =
        sampling match {
          case cs: ZipWithIndexSampling ⇒
            val proxy = SamplingProxyUI(new ZipWithIndexSamplingDataUI010(KeyRegistry.protoProxyKeyMap.get(PrototypeKey(cs.index))))
            (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.sampling), bSC))
          case _ ⇒ (SamplingProxyUI(buildDataUI), bSC)
        }
    }, new SamplingFactoryUI {
      def buildDataUI = new ZipWithNameSamplingDataUI010

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = sampling match {
        case cs: ZipWithNameSampling ⇒
          val proxy = SamplingProxyUI(new ZipWithNameSamplingDataUI010(KeyRegistry.protoProxyKeyMap.get(PrototypeKey(cs.name))))
          (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.factor), bSC))
        case _ ⇒ (SamplingProxyUI(buildDataUI), bSC)
      }
    })

}
