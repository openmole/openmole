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
package org.openmole.gui.plugin.wizard.java

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import org.scalajs.dom.raw.HTMLElement
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object TopLevelExports {
  @JSExportTopLevel("wizard_java")
  val r = js.Object {
    new org.openmole.gui.plugin.wizard.java.JavaWizardFactory
  }
}

class JavaWizardFactory extends WizardPluginFactory:

  override def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AcceptedModel]] = Future.successful {
    WizardUtils.findFileWithExtensions(
      uploaded,
      "jar" -> FindLevel.SingleRoot,
      "jar" -> FindLevel.Level1
    )
  }

  override def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("jar" , _, _) => Future.successful(ModelMetadata(command = Some(s"""// Call a method here""")))
      case _ => WizardUtils.unknownError(accepted, name)

  override def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[GeneratedModel] =
    accepted match
      case AcceptedModel("jar", _, files) =>
        val taskName = WizardUtils.toTaskName(files.head._1)

        def set = WizardUtils.mkSet(
          modelMetadata
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  JavaTask(\"\"\"${modelMetadata.command.getOrElse("")}\"\"\", jars = Seq(${files.map((f, _) => WizardUtils.inWorkDirectory(f)).mkString(", ")})) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(files.head._1))
          )

        Future.successful(script)
      case _ => WizardUtils.unknownError(accepted, name)

  def name: String = "Java"
