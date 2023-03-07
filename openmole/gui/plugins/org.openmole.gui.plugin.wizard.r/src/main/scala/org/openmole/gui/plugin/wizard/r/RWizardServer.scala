package org.openmole.gui.plugin.wizard.r

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
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.DataUtils.*
import org.openmole.tool.file.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*

import util.{Failure, Success, Try}

class RWizardServer(s: Services) extends APIServer with RWizardAPI {

  val toTaskRoute =
    toTask.errorImplementedBy { case (p, m) => impl.toTask(p, m) }

  val parseRoute =
    parse.errorImplementedBy { p => impl.parse(p) }

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(toTaskRoute, parseRoute)
  )

  object impl {
    import s._

    def toTask(target: SafePath, modelMetadata: ModelMetadata): Unit = {

      val modelData = WizardUtils.wizardModelData(modelMetadata.inputs, modelMetadata.outputs, Some("inputs"), Some("ouputs"))

      val task = s"${
        modelMetadata.executableName.map {
          _.split('.').toSeq
        }.getOrElse(Seq()).head.toLowerCase
      }Task"

      val content = modelData.vals +
        s"""\nval $task = RTask(\"\"\"\n   source("${modelMetadata.executableName.getOrElse("")}")\n   \"\"\") set(\n""".stripMargin +
        WizardUtils.expandWizardData(modelData) +
        s""")\n\n$task hook ToStringHook()"""

      target.toFile.content = content
    }

    def parse(safePath: SafePath): Option[ModelMetadata] = None

  }
}
