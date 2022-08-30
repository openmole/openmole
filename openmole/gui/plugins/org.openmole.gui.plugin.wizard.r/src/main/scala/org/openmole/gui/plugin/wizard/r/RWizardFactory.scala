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
package org.openmole.gui.plugin.wizard.r

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import com.raquo.laminar.api.L._
import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("r")
  val r = js.Object {
    new org.openmole.gui.plugin.wizard.r.RWizardFactory
  }
}

class RWizardFactory extends WizardPluginFactory {
  val fileType = CodeFile(RLanguage())

  def parse(safePath: SafePath): Future[Option[ModelMetadata]] = PluginFetch.future(_.parse(safePath).future)

  def toTask(safePath: SafePath, modelMetadata: ModelMetadata) = PluginFetch.future(_.toTask(safePath, modelMetadata).future)

  def name: String = "R"
}