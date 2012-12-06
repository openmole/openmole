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

import org.openmole.ide.core.implementation.registry.OSGiActivator
import org.openmole.ide.core.implementation.registry.SamplingActivator
import org.openmole.ide.core.model.factory.ISamplingFactoryUI

class Activator extends OSGiActivator with SamplingActivator {

  override def samplingFactories = List(
    new ISamplingFactoryUI {
      def buildDataUI = new CompleteSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new CombineSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new ShuffleSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new TakeSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipWithIndexSamplingDataUI
    }, new ISamplingFactoryUI {
      def buildDataUI = new ZipWithNameSamplingDataUI
    })
}
