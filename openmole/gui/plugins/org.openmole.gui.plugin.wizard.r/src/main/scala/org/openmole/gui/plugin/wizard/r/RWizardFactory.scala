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
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("r")
  val r = js.Object {
    new org.openmole.gui.plugin.wizard.r.RWizardFactory
  }
}

class RWizardFactory extends WizardPluginFactory:
  override def editable: Seq[FileContentType] =
    val R = ReadableFileType(Seq("r", "R"), text = true)
    Seq(R)

  def accept(uploaded: Seq[(RelativePath, SafePath)]) = uploaded.filter(_._1.value.size < 2).exists(_._1.name.endsWith(".R"))
  def parse(uploaded: Seq[(RelativePath, SafePath)])(using basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] = ???  //PluginFetch.futureError(_.parse(safePath).future)
  def content(uploaded: Seq[(RelativePath, SafePath)], modelMetadata: ModelMetadata)(using basePath: BasePath, notificationAPI: NotificationService) = ??? //PluginFetch.futureError(_.toTask(safePath, modelMetadata).future)
  def name: String = "R"
