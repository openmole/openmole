/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.pluginmanager._
import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.BundleContext

class Activator extends PluginInfoActivator {

  override def stop(context: BundleContext): Unit = {
    PluginInfo.unregister(this)
    ConfigurationLocationRegistry.unregister(this)
  }

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.pluginmanager.KeyWord._

    val keyWords: Vector[KeyWord] =
      Vector(
        TaskKeyWord(objectName(BreedTask)),
        TaskKeyWord(objectName(DeltaTask)),
        TaskKeyWord(objectName(ElitismTask)),
        TaskKeyWord(objectName(FromIslandTask)),
        TaskKeyWord(objectName(GenerateIslandTask)),
        TaskKeyWord(objectName(ScalingGenomeTask)),
        TaskKeyWord(objectName(TerminationTask)),
        TaskKeyWord(objectName(InitialStateTask)),
        PatternKeyWord(objectName(GenomeProfileEvolution)),
        PatternKeyWord(objectName(NSGA2Evolution)),
        PatternKeyWord(objectName(OSEEvolution)),
        PatternKeyWord(objectName(PSEEvolution)),
        WordKeyWord(Stochastic.getClass),
        HookKeyWord(objectName(SavePopulationHook))
      )

    PluginInfo.register(this, Vector(this.getClass.getPackage), keyWords = keyWords)
    ConfigurationLocationRegistry.register(
      this,
      ConfigurationLocationRegistry.list()
    )
  }
}