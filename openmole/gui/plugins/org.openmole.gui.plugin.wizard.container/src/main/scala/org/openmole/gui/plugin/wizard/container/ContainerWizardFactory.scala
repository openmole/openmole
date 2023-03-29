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
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports:
  @JSExportTopLevel("wizard_container")
  val container = js.Object {
    new ContainerWizardFactory
  }

class ContainerWizardFactory extends WizardPluginFactory:
  def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) =
    if uploaded.size == 1
    then
      val name = uploaded.head._1.name
      name.endsWith(".tar") || name.endsWith(".tgz") || name.endsWith(".tar.gz") || name.endsWith(".sh")
    else
      if WizardUtils.singleFolderContaining(uploaded, _._1.name.endsWith(".sh")).isDefined
      then true
      else false

  def parse(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] = Future(ModelMetadata()) //PluginFetch.futureError(_.parse(safePath).future)

  def content(uploaded: Seq[(RelativePath, SafePath)], modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) =
    WizardUtils.singleFolderContaining(uploaded, _._1.name.endsWith(".sh")) match
      case Some(s) =>
        val taskName = WizardUtils.toTaskName(s._1)

        def set = WizardUtils.mkSet(
          modelMetadata,
          s"resources += (workDirectory / \"${s._1.parent.mkString}\")"
        )

        def script =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", ${modelMetadata.command.getOrElse(s"""\"bash '${s._1.mkString}'\"""")}, install = Seq("apt update", "apt install bash", "apt clean")) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(s._1))
          )

        Future.successful(script)
      case None =>
        val file = uploaded.head._1
        val taskName = WizardUtils.toTaskName(file)

        def container =
          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask(workDirectory / "${file.mkString}", "${modelMetadata.command.getOrElse("echo Viva OpenMOLE")}") ${WizardUtils.mkSet(modelMetadata)}
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )

        def script =
          def set = WizardUtils.mkSet(
            modelMetadata,
            s"resources += (workDirectory / \"${file.mkString}\")"
          )

          GeneratedModel(
            s"""
               |${WizardUtils.preamble}
               |
               |${WizardUtils.mkVals(modelMetadata)}
               |val $taskName =
               |  ContainerTask("debian:stable-slim", ${modelMetadata.command.getOrElse(s"""\"bash '${file.mkString}'\"""")}, install = Seq("apt update", "apt install bash", "apt clean")) $set
               |
               |$taskName""".stripMargin,
            Some(WizardUtils.toOMSName(file))
          )

        if file.name.endsWith(".sh")
        then Future.successful(script)
        else Future.successful(container)

  def name: String = "Container"


