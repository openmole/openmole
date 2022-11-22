package org.openmole.gui.plugin.wizard.netlogo

/*
 * Copyright (C) 2022 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.services.Services
import org.openmole.gui.ext.server.{APIServer, WizardUtils, utils}
import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.gui.ext.data.*
import org.openmole.gui.ext.data.DataUtils.*
import org.openmole.tool.file.*
import org.openmole.gui.ext.server.*
import org.openmole.gui.ext.server.utils.*

import util.{Failure, Success, Try}

class NetlogoWizardServer(s: Services) extends APIServer with NetlogoWizardAPI {

  val toTaskRoute =
    toTask.implementedBy { case(p, m) => impl.toTask(p, m) }

  val parseRoute =
    parse.implementedBy { p => impl.parse(p) }

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(toTaskRoute, parseRoute )
  )

  object impl {

    import s._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    def toTask(target: SafePath, mmd: ModelMetadata): Unit = {

      //  val modelMetadata = parse(target)
      import org.openmole.gui.ext.data.ServerFileSystemContext.project

      val modelData = WizardUtils.wizardModelData(mmd.inputs, mmd.outputs, Some("inputs"), Some("outputs"))
      val task = s"${
        mmd.executableName.map {
          _.split('.').head.toLowerCase
        }.getOrElse("")
      }Task"

      val embeddWS = mmd.sourcesDirectory.toFile.listFiles.exists(f => f.getName.contains("netlogo") || f.getName.contains("nls"))
      val targetFile = (target / (task + ".oms")).toFile

      val content = modelData.vals +
        s"""\n\nval launch = List("${
          mmd.command.map {
            _.split('\n').toSeq.mkString("\", \"")
          }.getOrElse("")
        }")
              \nval $task = NetLogo6Task(\n  workDirectory / ${
        mmd.executableName.map {
          _.split('/').toSeq
        }.getOrElse(Seq()).map { s ⇒ s"""\"$s\"""" }.mkString(" / ")
      },\n  launch,\n  embedWorkspace = ${embeddWS},\n  seed = mySeed\n) set (\n""".stripMargin +
        WizardUtils.expandWizardData(modelData) +
        s"""\n)\n\n$task hook display"""


      targetFile.content = content


      // WizardToTask(target)
    }

    def parse(safePath: SafePath): Option[ModelMetadata] = {

      val lines = safePath.toFile.lines

      def parse0(lines: Seq[(String, Int)], args: Seq[PrototypePair], outputs: Seq[PrototypePair]): (Seq[PrototypePair], Seq[PrototypePair]) = {
        if (lines.isEmpty) (PrototypePair("mySeed", PrototypeData.Long, "0", None) +: args, outputs)
        else {
          val (line, index) = lines.head
          val tail = lines.tail
          if (line.startsWith("SLIDER")) parse0(tail, args :+ parseSlider(index), outputs)
          else if (line.startsWith("SWITCH")) parse0(tail, args :+ parseSwitch(index), outputs)
          else if (line.startsWith("INPUTBOX")) parse0(tail, args :+ parseInputBox(index), outputs)
          else if (line.startsWith("CHOOSER")) parse0(tail, args :+ parseChooser(index), outputs)
          else if (line.startsWith("MONITOR")) parse0(tail, args, outputs ++ parseMonitor(index))
          // else if (line.startsWith("PLOT")) parse0(tail, args, outputs ++ parsePlot(index))
          else parse0(tail, args, outputs)
        }
      }

      def parseSlider(start: Int): PrototypePair = {
        val name = lines(start + 5)
        PrototypePair(name.clean, PrototypeData.Double, lines(start + 9), Some(name))
      }

      def parseSwitch(start: Int): PrototypePair = {
        val name = lines(start + 5)
        PrototypePair(name.clean, PrototypeData.Boolean, lines(start + 7), Some(name))
      }

      def parseInputBox(start: Int): PrototypePair = {
        val name = lines(start + 5)
        PrototypePair(name.clean, PrototypeData.Double, lines(start + 6), Some(name))
      }

      def parseMonitor(start: Int): Seq[PrototypePair] = {
        val name = lines(start + 6).split(' ')
        if (name.size == 1) Seq(PrototypePair(name.head.clean, PrototypeData.Double, mapping = Some(name.head)))
        else Seq()
      }

      def parseChooser(start: Int): PrototypePair = {
        val name = lines(start + 5)
        PrototypePair(name.clean, PrototypeData.String, lines(start + 7).split(' ').head, Some(name))
      }

      val (inputs, outputs) = parse0(lines.toSeq.zipWithIndex, Seq(), Seq())

      Some(ModelMetadata(
        Some(NetLogoLanguage()),
        inputs,
        outputs,
        None,
        Some(safePath.name),
        safePath.parent
      ))
    }

  }

}
