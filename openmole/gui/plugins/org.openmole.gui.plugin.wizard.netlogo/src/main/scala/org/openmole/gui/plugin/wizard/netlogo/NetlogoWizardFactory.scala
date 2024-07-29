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
package org.openmole.gui.plugin.wizard.netlogo

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
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports:
  @JSExportTopLevel("wizard_netlogo")
  val netlogo = js.Object {
    new org.openmole.gui.plugin.wizard.netlogo.NetlogoWizardFactory
  }

class NetlogoWizardFactory extends WizardPluginFactory:

  def parseContent(content: String): ModelMetadata =
    val lines: Array[String] = content.linesIterator.toArray

    def parseSlider(start: Int): PrototypeData =
      val name = lines(start + 5)
      PrototypeData(WizardUtils.toVariableName(name), PrototypeData.Double, lines(start + 9), Some(name))

    def parseSwitch(start: Int): PrototypeData =
      val name = lines(start + 5)
      val default = if lines(start + 7).trim == "1" then "true" else "false"
      println(lines(start + 7) + " " + default)
      PrototypeData(WizardUtils.toVariableName(name), PrototypeData.Boolean, default, Some(name))

    def parseInputBox(start: Int): PrototypeData =
      val name = lines(start + 5)
      PrototypeData(WizardUtils.toVariableName(name), PrototypeData.Double, lines(start + 6), Some(name))

    def parseMonitor(start: Int): Seq[PrototypeData] =
      lines(start + 6).replace("\\n", "").split(' ').headOption match
        case Some(name) => Seq(PrototypeData(WizardUtils.toVariableName(name), PrototypeData.Double, mapping = Some(name)))
        case None => Seq()

    def parseChooser(start: Int): PrototypeData =
      val name = lines(start + 5)
      PrototypeData(WizardUtils.toVariableName(name), PrototypeData.String, lines(start + 7).split(' ').head, Some(name))

    def parse0(lines: Seq[(String, Int)], inputs: Seq[PrototypeData], outputs: Seq[PrototypeData]): (Seq[PrototypeData], Seq[PrototypeData]) =
      if lines.isEmpty
      then (PrototypeData("mySeed", PrototypeData.Long, "0", None) +: inputs, outputs)
      else
        val (line, index) = lines.head
        val tail = lines.tail
        if (line.startsWith("SLIDER")) parse0(tail, inputs :+ parseSlider(index), outputs)
        else if (line.startsWith("SWITCH")) parse0(tail, inputs :+ parseSwitch(index), outputs)
        else if (line.startsWith("INPUTBOX")) parse0(tail, inputs :+ parseInputBox(index), outputs)
        else if (line.startsWith("CHOOSER")) parse0(tail, inputs :+ parseChooser(index), outputs)
        else if (line.startsWith("MONITOR")) parse0(tail, inputs, outputs ++ parseMonitor(index))
        // else if (line.startsWith("PLOT")) parse0(tail, args, outputs ++ parsePlot(index))
        else parse0(tail, inputs, outputs)


    val (inputs, outputs) = parse0(lines.toSeq.zipWithIndex, Seq(), Seq())

    ModelMetadata(
      inputs,
      outputs,
      command = Some(
        """setup
          |go""".stripMargin)
    )

  override def editable: Seq[FileContentType] =
    val NetLogo = ReadableFileType(Seq("nlogo", "nlogo3d", "nls"), text = true, highlight = Some("netlogo"))
    Seq(NetLogo)

  def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) = Future.successful {
    WizardUtils.findFileWithExtensions(
      uploaded,
      "nlogo" -> FindLevel.SingleFile,
      "nlogo" -> FindLevel.Directory,
      "nlogo" -> FindLevel.MultipleFile
    )
  }

  def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata] =
    accepted match
      case AcceptedModel("nlogo", _, f :: _) => api.download(f._2).map { (content, _) => parseContent(content) }
      case _ => WizardUtils.unknownError(accepted, name)


  def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) =
    accepted match
      case AcceptedModel("nlogo", level, (nlogo, _) :: _) =>
        val task = WizardUtils.toTaskName(nlogo)
        val (directory, file) =
          level match
            case FindLevel.MultipleFile =>
              val d = WizardUtils.toDirectoryName(nlogo)
              (Some(d), d :: nlogo)
            case _ => (None, nlogo)

        val embedWS =
          level == FindLevel.Directory || level == FindLevel.MultipleFile

        def params = WizardUtils.mkTaskParameters(
          WizardUtils.inWorkDirectory(file),
          WizardUtils.mkCommandString(modelMetadata.commandValue.split('\n')),
          "seed = mySeed",
          if embedWS then "embedWorkspace = true" else ""
        )

        val content =
          s"""
            |${WizardUtils.preamble}
            |
            |${WizardUtils.mkVals(modelMetadata)}
            |val mySeed = Val[Int]
            |
            |val $task = NetLogo6Task(
            |  $params) ${WizardUtils.mkSet(modelMetadata, "mySeed := 42")}
            |
            |$task
            |""".stripMargin

        Future.successful(
          GeneratedModel(
            content,
            Some(WizardUtils.toOMSName(nlogo._1)),
            directory
          )
        )
      case _ => WizardUtils.unknownError(accepted, name)


  def name: String = "NetLogo"


