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
package org.openmole.gui.plugin.wizard.container

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation.*
import com.raquo.laminar.api.L.*
import org.openmole.core.exception.InternalProcessingError
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports:
  @JSExportTopLevel("wizard_container")
  val container = js.Object {
    new ContainerWizardFactory
  }

class ContainerWizardFactory extends WizardPluginFactory:

  def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) = Future.successful {
    WizardUtils.findFileWithExtensions(
      uploaded,
      "tar" -> FindLevel.SingleFile,
      "tgz" -> FindLevel.SingleFile,
      "tar.gz" -> FindLevel.SingleFile,
      "sh" -> FindLevel.SingleFile,
      "sh" -> FindLevel.Directory,
      "sh" -> FindLevel.MultipleFile,
    )
  }

  def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("tar" | "tgz" | "tar.gz", _, _) => Future.successful(ModelMetadata(command = Some("echo Viva OpenMOLE")))
      case AcceptedModel("sh", _, f :: _) => Future.successful(ModelMetadata(command = Some(s"bash ${f._1.name}")))
      case _ => WizardUtils.unknownError(accepted, name)

  def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) =
    def installBash = WizardUtils.mkCommandString(Seq("apt update", "apt install bash", "apt clean"))

    accepted match
      case AcceptedModel("sh", FindLevel.SingleFile, (s, _) :: _) =>
        val taskName = WizardUtils.toTaskName(s)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(s)})"
        )

        val gm =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", \"${modelMetadata.commandValue}\", install = $installBash) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(s))
          )

        Future.successful(gm)
      case AcceptedModel("sh", FindLevel.MultipleFile, (s, _) :: _) =>
        val taskName = WizardUtils.toTaskName(s)
        val directory = WizardUtils.toDirectoryName(s)

        def cmd = Seq(s"cd $directory", modelMetadata.commandValue)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(directory)})"
        )

        val gm =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", ${WizardUtils.mkCommandString(cmd)}, install = $installBash) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(s)),
            directory = Some(directory)
          )

        Future.successful(gm)
      case AcceptedModel("sh", FindLevel.Directory, (s, _) :: _) =>
        val taskName = WizardUtils.toTaskName(s)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(s.parent)})"
        )

        def cmd = Seq(s"cd ${s.parent.name}", modelMetadata.commandValue)

        val gm =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", $cmd, install = $installBash) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(s))
          )

        Future.successful(gm)
      case AcceptedModel("tar" | "tgz" | "tar.gz", FindLevel.SingleFile, f :: _) =>
        val file = f._1
        val taskName = WizardUtils.toTaskName(file)

        def container =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask(workDirectory / "${file.mkString}", "${modelMetadata.command.getOrElse("")}") ${WizardUtils.mkSet(modelMetadata)}
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )

        Future.successful(container)
      case _ => WizardUtils.unknownError(accepted, name)

  def name: String = "Container"


