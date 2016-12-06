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
package org.openmole.gui.plugin.environment.egi

import org.openmole.gui.ext.plugin.{ PluginActivator, PluginInfo }
import org.openmole.gui.ext.tool.{ AutowireServer, OMRouter }

import scala.concurrent.ExecutionContext.Implicits.global

class Activator extends PluginActivator {
  /* with autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {

   def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

   def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)*/

  def info: PluginInfo = PluginInfo(
    classOf[EGIGUIAuthentication],
    OMRouter[API](AutowireServer.route[API](new APIImpl))
  )

  println("Info" + info)
  println(info.clientInstance)
}
