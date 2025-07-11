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

import org.openmole.core.highlight.HighLight
import org.openmole.core.pluginregistry.PluginRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator:

  override def stop(context: BundleContext): Unit =
    PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit =
    import org.openmole.core.highlight.HighLight._

    val keyWords: Vector[HighLight] =
      Vector(
        TaskHighLight(objectName(BreedTask)),
        TaskHighLight(objectName(DeltaTask)),
        TaskHighLight(objectName(ElitismTask)),
        TaskHighLight(objectName(FromIslandTask)),
        TaskHighLight(objectName(GenerateIslandTask)),
        TaskHighLight(objectName(ScalingGenomeTask)),
        TaskHighLight(objectName(TerminationTask)),
        TaskHighLight(objectName(InitialStateTask)),
        PatternHighLight(objectName(ProfileEvolution)),
        PatternHighLight(objectName(NSGA2Evolution)),
        PatternHighLight(objectName(NSGA3Evolution)),
        PatternHighLight(objectName(OSEEvolution)),
        PatternHighLight(objectName(PSEEvolution)),
        PatternHighLight(objectName(PPSEEvolution)),
        PatternHighLight(objectName(HDOSEEvolution)),
        ObjectHighLight(objectName(Stochastic)),
        ObjectHighLight(objectName(Island)),
        ObjectHighLight(objectName(Suggestion))
      )

    PluginRegistry.register(
      this,
      Vector(this.getClass.getPackage),
      highLight = keyWords,
      methodNames = Vector(EvolutionMetadata.method)
    )
