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
      "tar" -> FindLevel.SingleRoot,
      "tgz" -> FindLevel.SingleRoot,
      "tar.gz" -> FindLevel.SingleRoot,
      "sh" -> FindLevel.SingleRoot,
      "sh" -> FindLevel.Level1
    )
  }

  def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("tar" | "tgz" | "tar.gz", _, _) => Future.successful(ModelMetadata(command = Some("echo Viva OpenMOLE")))
      case AcceptedModel("sh", _, f :: _) => Future.successful(ModelMetadata(command = Some(s"bash ${f._1.name}")))
      case _ => WizardUtils.unknownError(accepted, name)

  def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) =
    accepted match
      case AcceptedModel("sh", FindLevel.Level1, shell :: _) =>
        val taskName = WizardUtils.toTaskName(shell._1)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (${WizardUtils.inWorkDirectory(shell._1.parent)})"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", ${modelMetadata.command.getOrElse(s"")}, install = Seq("apt update", "apt install bash", "apt clean")) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(shell._1))
          )

        Future.successful(script)
      case AcceptedModel("sh", FindLevel.SingleRoot, f :: _) =>
        val file = f._1
        val taskName = WizardUtils.toTaskName(file)
        def script =
          def set = WizardUtils.mkSet(
            modelMetadata,
            s"resources += (${WizardUtils.inWorkDirectory(file)})"
          )

          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", ${modelMetadata.command.getOrElse(s"")}, install = Seq("apt update", "apt install bash", "apt clean")) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )

        Future.successful(script)

      case AcceptedModel("tar" | "tgz" | "tar.gz", FindLevel.SingleRoot, f :: _) =>
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
//        def java =
//          def set = WizardUtils.mkSet(
//            modelMetadata,
//            s"resources += (workDirectory / \"${file.mkString}\")"
//          )
//
//          GeneratedModel(
//            s"""
//               |${WizardUtils.preamble}
//               |
//               |${WizardUtils.mkVals(modelMetadata)}
//               |val $taskName =
//               |  ContainerTask("openjdk:17-jdk-slim", ${modelMetadata.command.getOrElse(s"""\"bash '${file.mkString}'\"""")}, install = Seq("apt update", "apt install bash", "apt clean")) $set
//               |
//               |$taskName""".stripMargin,
//            Some(WizardUtils.toOMSName(file))
//          )


  def name: String = "Container"


