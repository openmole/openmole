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
package org.openmole.gui.plugin.wizard.julia

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
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("wizard_julia")
  val julia = js.Object {
    new org.openmole.gui.plugin.wizard.julia.JuliaWizardFactory
  }
}

object JuliaWizardFactory:
  def resourceLocation(file: RelativePath, level: FindLevel) =
    level match
      case FindLevel.SingleFile => file
      case FindLevel.MultipleFile => WizardUtils.toDirectoryName(file) :: file
      case FindLevel.Directory => file

class JuliaWizardFactory extends WizardPluginFactory:
  override def editable: Seq[FileContentType] = Seq(ReadableFileType(Seq("jl"), text = true))

  override def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AcceptedModel]] = Future.successful {
    WizardUtils.findFileWithExtensions(
      uploaded,
      "jl" -> FindLevel.SingleFile,
      "jl" -> FindLevel.Directory,
      "jl" -> FindLevel.MultipleFile
    )
  }

  override def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("jl" , level, f :: _) => Future.successful(ModelMetadata(command = Some(s"""include("${JuliaWizardFactory.resourceLocation(f._1, level).mkString}")""")))
      case _ => WizardUtils.unknownError(accepted, name)

  override def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[GeneratedModel] =
    accepted match
      case AcceptedModel("jl", FindLevel.SingleFile, (file, _) :: _) =>
        val taskName = WizardUtils.toTaskName(file)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(file)})"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  JuliaTask(${modelMetadata.quotedCommandValue}) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )

        Future.successful(script)
      case AcceptedModel("jl", FindLevel.MultipleFile, (file, _) :: _) =>
        val taskName = WizardUtils.toTaskName(file)
        val directory = WizardUtils.toDirectoryName(file)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(directory)})"
        )

        def parameters = WizardUtils.mkTaskParameters(
          modelMetadata.quotedCommandValue,
          s"prepare = ${WizardUtils.mkCommandString(Seq(s"cd $directory"))}"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  JuliaTask($parameters) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file)),
            Some(directory)
          )

        Future.successful(script)
      case AcceptedModel("jl", FindLevel.Directory, (file, _) :: _) =>
        val taskName = WizardUtils.toTaskName(file)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(file.parent)})"
        )

        def parameters = WizardUtils.mkTaskParameters(
          modelMetadata.quotedCommandValue,
          s"prepare = ${WizardUtils.mkCommandString(Seq(s"cd ${file.parent.value}"))}"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  JuliaTask($parameters) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )
        Future.successful(script)
      case _ => WizardUtils.unknownError(accepted, name)

  def name: String = "Julia"
