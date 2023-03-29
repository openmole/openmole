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
import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import util.{Failure, Success, Try}

class NetlogoWizardServer(s: Services) extends APIServer with NetlogoWizardAPI {

  val toTaskRoute =
    toTask.errorImplementedBy { case(p, m) => ??? } // impl.toTask(p, m) }

  val parseRoute =
    parse.errorImplementedBy { p => ??? } //impl.parse(p) }

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(toTaskRoute, parseRoute )
  )

//  object impl {
//
//    import s._

//    def toTask(target: SafePath, mmd: ModelMetadata): Unit = {
//
//      //  val modelMetadata = parse(target)
//
//      val modelData = WizardUtils.wizardModelData(mmd.inputs, mmd.outputs, Some("inputs"), Some("outputs"))
//      val task = s"${
//        mmd.executableName.map {
//          _.split('.').head.toLowerCase
//        }.getOrElse("")
//      }Task"
//
//      val embeddWS = mmd.sourcesDirectory.toFile.listFiles.exists(f => f.getName.contains("netlogo") || f.getName.contains("nls"))
//      val targetFile = (target / (task + ".oms")).toFile
//
//      val content = modelData.vals +
//        s"""\n\nval launch = List("${
//          mmd.command.map {
//            _.split('\n').toSeq.mkString("\", \"")
//          }.getOrElse("")
//        }")
//              \nval $task = NetLogo6Task(\n  workDirectory / ${
//        mmd.executableName.map {
//          _.split('/').toSeq
//        }.getOrElse(Seq()).map { s ⇒ s"""\"$s\"""" }.mkString(" / ")
//      },\n  launch,\n  embedWorkspace = ${embeddWS},\n  seed = mySeed\n) set (\n""".stripMargin +
//        WizardUtils.expandWizardData(modelData) +
//        s"""\n)\n\n$task hook display"""
//
//
//      targetFile.content = content
//
//
//      // WizardToTask(target)
//    }



}
