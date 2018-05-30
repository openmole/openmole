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
package org.openmole.gui.plugin.versioning.git

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.openmole.gui.ext.tool.client
import org.scalajs.dom.raw.HTMLElement
import scaladget.bootstrapnative.SelectableButtons

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitFactory")
class GitFactory extends VersioningPluginFactory {

  def name: String = "GIT"

  def build(cloneIn: SafePath) = new GitGUI(cloneIn)
}

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitGUI")
class GitGUI(cloneIn: SafePath) extends VersioningGUIPlugin {

  def factory = new GitFactory

  lazy val panel: TypedTag[HTMLElement] = div("Lorem ipsum dolor sit amet, " +
    "consectetur adipiscing elit, sed do eiusmod tempor " +
    "incididunt ut labore et dolore magna aliqua. " +
    "Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
    "laboris nisi ut aliquip ex ea commodo consequat. Duis aute " +
    "irure dolor in reprehenderit in voluptate velit esse cillum dolore " +
    "eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, " +
    "sunt in culpa qui officia deserunt mollit anim id est laborum.", padding := 10)

  def clone(
    url: String) =
    OMPost()[GitAPI].clone(
      url).call()

}