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
package org.openmole.gui.shared.data

import com.raquo.laminar.api.L._
import scala.concurrent.Future

sealed trait GUIPlugin

trait AuthenticationPlugin extends GUIPlugin {
  type AuthType <: AuthenticationData

  def data: AuthType

  def factory: AuthenticationPluginFactory

  def panel: HtmlElement

  def save(onsave: () ⇒ Unit): Unit

  def remove(onremoved: () ⇒ Unit): Unit

  def test: Future[Seq[Test]]
}

sealed trait GUIPluginFactory

trait AuthenticationPluginFactory extends GUIPluginFactory {
  type AuthType <: AuthenticationData

  def name: String

  def build(data: AuthType): AuthenticationPlugin

  def buildEmpty: AuthenticationPlugin

  def getData: Future[Seq[AuthType]]
}

trait WizardGUIPlugin extends GUIPlugin {
  def factory: WizardPluginFactory

  val panel: HtmlElement

  def save(): Unit
}

trait WizardPluginFactory extends GUIPluginFactory {
  def name: String

  def fileType: FileType

  def parse(safePath: SafePath): Future[Option[ModelMetadata]]
  
  def toTask(safePath: SafePath, modelMetadata: ModelMetadata): Future[Unit]
}

trait MethodAnalysisPlugin extends GUIPlugin {
  def panel(safePath: SafePath, services: PluginServices): HtmlElement
}

case class PluginServices(errorManager: ErrorManager)

trait ErrorManager {
  def signal(message: String, stack: Option[String] = None): Unit
}
