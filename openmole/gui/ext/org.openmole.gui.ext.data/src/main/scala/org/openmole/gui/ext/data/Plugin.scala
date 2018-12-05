/**
 * Created by Romain Reuillon on 28/11/16.
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
package org.openmole.gui.ext.data

import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scalatags.JsDom.TypedTag
import org.openmole.core.services._

sealed trait GUIPlugin

trait AuthenticationPlugin extends GUIPlugin {
  type AuthType <: AuthenticationData

  def data: AuthType

  def factory: AuthenticationPluginFactory

  def panel: TypedTag[HTMLElement]

  def save(onsave: () ⇒ Unit): Unit

  def remove(onremoved: () ⇒ Unit): Unit

  def test: Future[Seq[Test]]
}

trait VersioningAPI extends PluginAPI {
  def clone(url: String, folder: SafePath): Option[MessageError]

  def modifiedFiles(safePath: SafePath): Seq[SafePath]
}

sealed trait GUIPluginFactory {

  type FactoryPluginAPI <: PluginAPI

  def api: Services ⇒ FactoryPluginAPI

  def name: String
}

trait AuthenticationPluginFactory extends GUIPluginFactory {
  type AuthType <: AuthenticationData

  type FactoryPluginAPI = PluginAPI

  def build(data: AuthType): AuthenticationPlugin

  def buildEmpty: AuthenticationPlugin

  def getData: Future[Seq[AuthType]]
}

trait WizardGUIPlugin extends GUIPlugin {

  def factory: WizardPluginFactory

  val panel: TypedTag[HTMLElement]

  def save(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources): Future[WizardToTask]
}

trait WizardPluginFactory extends GUIPluginFactory {

  type FactoryPluginAPI = PluginAPI

  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit): WizardGUIPlugin

  def fileType: FileType

  def parse(safePath: SafePath): Future[Option[LaunchingCommand]]
}

trait VersioningGUIPlugin extends GUIPlugin {

  def factory: VersioningPluginFactory

  val panel: TypedTag[HTMLElement]
}

trait VersioningPluginFactory extends GUIPluginFactory {

  override type FactoryPluginAPI = VersioningAPI

  def build(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}): VersioningGUIPlugin

  def versioningConfigFolderName: String
}