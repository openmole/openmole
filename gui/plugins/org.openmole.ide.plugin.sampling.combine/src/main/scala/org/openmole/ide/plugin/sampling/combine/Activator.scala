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

package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.implementation.builder._
import org.openmole.ide.core.model.factory.{ IPrototypeFactoryUI, ISamplingFactoryUI }
import org.openmole.core.model.sampling.Sampling
import org.openmole.plugin.sampling.combine._
import org.openmole.ide.core.model.sampling.IBuiltCompositionSampling
import org.openmole.ide.core.implementation.sampling.SamplingProxyUI

class Activator extends OSGiActivator with SamplingActivator {
  override def samplingFactories = List(
    new ISamplingFactoryUI {

      def buildDataUI = new CompleteSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = new SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: CompleteSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, cs.samplings, bSC))
          case _ ⇒ (proxy, bSC)
        }
      }
    }, new ISamplingFactoryUI {
      def buildDataUI = new CombineSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = new SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: CombineSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, cs.samplings, bSC))
          case _ ⇒ (proxy, bSC)
        }
      }
    }, new ISamplingFactoryUI {
      def buildDataUI = new ShuffleSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = new SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: ShuffleSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.sampling), bSC))
          case _ ⇒ (proxy, bSC)
        }
      }
    }, new ISamplingFactoryUI {
      def buildDataUI = new TakeSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        sampling match {
          case cs: TakeSampling ⇒
            val proxy = new SamplingProxyUI(new TakeSamplingDataUI(cs.n.toString))
            (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.sampling), bSC))
          case _ ⇒ (new SamplingProxyUI(buildDataUI), bSC)
        }
      }
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = {
        val proxy = new SamplingProxyUI(buildDataUI)
        sampling match {
          case cs: ZipSampling ⇒ (proxy, Builder.buildConnectedSamplings(proxy, cs.samplings, bSC))
          case _ ⇒ (proxy, bSC)
        }
      }
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipWithIndexSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) =
        sampling match {
          case cs: ZipWithIndexSampling ⇒
            val proxy = new SamplingProxyUI(new ZipWithIndexSamplingDataUI(KeyRegistry.protoProxyKeyMap.get(KeyPrototypeGenerator(cs.index))))
            (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.reference), bSC))
          case _ ⇒ (new SamplingProxyUI(buildDataUI), bSC)
        }
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipWithNameSamplingDataUI

      def fromCoreObject(sampling: Sampling,
                         bSC: IBuiltCompositionSampling) = sampling match {
        case cs: ZipWithNameSampling ⇒
          val proxy = new SamplingProxyUI(new ZipWithNameSamplingDataUI((KeyRegistry.protoProxyKeyMap.get(KeyPrototypeGenerator(cs.name)))))
          (proxy, Builder.buildConnectedSamplings(proxy, Seq(cs.factor), bSC))
        case _ ⇒ (new SamplingProxyUI(buildDataUI), bSC)
      }
    })

}
