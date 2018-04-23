/**
 * Created by Mathieu Leclaire on 23/04/18.
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
package org.openmole.gui.plugin.wizard.netlogo

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory, WizardPluginFactory, _ }
import org.openmole.gui.ext.tool.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

@JSExportTopLevel("org.openmole.gui.plugin.wizard.netlogo.NetlogoWizardFactory")
class NetlogoWizardFactory extends WizardPluginFactory {
  val extension = FileExtension.NETLOGO

  def build: WizardPlugin = new NetlogoWizardGUI()

  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[NetlogoWizardAPI].parse(safePath).call()

  def toTask(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources): Future[SafePath] = OMPost()[NetlogoWizardAPI].toTask(target, executableName, command, inputs, outputs, libraries, resources).call()

  def help: String = "If your Netlogo sript depends on plugins, you should upload an archive (tar.gz, tgz) containing the root workspace."

  def name: String = "Netlogo"
}

@JSExportTopLevel("org.openmole.gui.plugin.wizard.netlogo.NetlogoWizardGUI")
class NetlogoWizardGUI() extends WizardPlugin {

  def factory = new NetlogoWizardFactory

  def panel: TypedTag[HTMLElement] = div()
}