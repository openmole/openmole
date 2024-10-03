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
package org.openmole.gui.client.ext

import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*

import scala.concurrent.Future

sealed trait GUIPlugin

trait AuthenticationPlugin[T] extends GUIPlugin:
  def name: String
  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): HtmlElement
  def save(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]

trait GUIPluginFactory

trait AuthenticationPluginFactory extends GUIPluginFactory:
  type AuthType
  def name: String
  def build(data: AuthType): AuthenticationPlugin[AuthType]
  def buildEmpty: AuthenticationPlugin[AuthType]
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]]
  def test(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]]
  def remove(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]

trait MethodAnalysisPlugin extends GUIPlugin:
  def panel(safePath: SafePath, services: PluginServices)(using basePath: BasePath, notificationAPI: NotificationService): HtmlElement

case class PluginServices(errorManager: ErrorManager)

trait ErrorManager:
  def signal(message: String, stack: Option[String] = None): Unit

case class GUIPlugins(
  authenticationFactories: Seq[AuthenticationPluginFactory],
  wizardFactories: Seq[wizard.WizardPluginFactory],
  analysisPlugins: Map[String, MethodAnalysisPlugin])
