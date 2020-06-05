/**
 * Created by Romain Reuillon on 29/11/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.plugin.server.GUIPlugin
import org.openmole.gui.ext.tool.server.{ AutowireServer, OMRouter }
import org.openmole.plugin.method.evolution.EvolutionMetadata
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  def info: GUIPlugin = GUIPlugin(
    analysis = Some(EvolutionMetadata.method -> classOf[EvolutionAnalysis]),
    router = Some(s ⇒ OMRouter[EvolutionAnalysisAPI](AutowireServer.route[EvolutionAnalysisAPI](new EvolutionAnalysisAPIImpl)))
  )

  override def start(context: BundleContext): Unit = GUIPlugin.register(this, info)
  override def stop(context: BundleContext): Unit = GUIPlugin.unregister(this)
}
