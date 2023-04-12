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
package org.openmole.gui.plugin.wizard.gama

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
  @JSExportTopLevel("wizard_gama")
  val gama = js.Object {
    new org.openmole.gui.plugin.wizard.gama.GAMAWizardFactory
  }
}

class GAMAWizardFactory extends WizardPluginFactory:
  def parseContent(content: String) =
    val experiment =
      content.linesIterator.find(l => l.trim.startsWith("experiment") && l.contains("gui")) match
        case Some(e) => e.trim.split(' ').filter(_.nonEmpty).drop(1).headOption
        case None => None
    ModelMetadata(command = experiment.orElse(Some(s"""experiment name""")))

  override def editable: Seq[FileContentType] = Seq(ReadableFileType(Seq("gaml"), text = true))

  override def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AcceptedModel]] = Future.successful {
    WizardUtils.findFileWithExtensions(
      uploaded,
      "gaml" -> FindLevel.SingleFile,
      "gaml" -> FindLevel.Directory,
      "gaml" -> FindLevel.MultipleFile
    )
  }

  override def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("gaml" , _, f :: _) => api.download(f._2).map { (content, _) => parseContent(content) }
      case _ => WizardUtils.unknownError(accepted, name)

  override def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[GeneratedModel] =
    val mySeed = PrototypeData("mySeed", PrototypeData.Long)

    accepted match
      case AcceptedModel("gaml", level, (file, _) :: _) =>
        val taskName = WizardUtils.toTaskName(file)
        val directory =
          level match
            case FindLevel.SingleFile | FindLevel.MultipleFile => WizardUtils.toDirectoryName(file)
            case FindLevel.Directory => file.parent.name

        def parameters = WizardUtils.mkTaskParameters(
          WizardUtils.inWorkDirectory(directory),
          s"gaml = ${WizardUtils.quoted(file.name)}",
          s"experiment = ${modelMetadata.quotedCommandValue}",
          s"finalStep = 1000",
          s"seed = ${mySeed.name}"
        )

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"${mySeed.name} := 42"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata, mySeed)}
               |val $taskName =
               |  GAMATask($parameters) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file)),
            Some(directory)
          )

        Future.successful(script)
      case _ => WizardUtils.unknownError(accepted, name)

  def name: String = "R"
