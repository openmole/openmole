package org.openmole.gui.plugin.analysis.evolution


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
import org.openmole.gui.shared.data.*
import org.openmole.plugin.method.evolution._
import org.openmole.core.services.Services
import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*

class EvolutionAnalysisServer(services: Services):
  val routes: HttpRoutes[IO] =
    routesFromEndpoints(
      EvolutionAnalysisAPI.analyse.implementedBy(impl.analyse)
    )

  object impl:
    def analyse(path: SafePath) = ???

//    def analyse(path: SafePath) = {
//      import ServerFileSystemContext.project
//      import services._
//
//      try {
//        val (omrData, methodData) = Analysis.loadMetadata(path.toFile)
//        Right(Analysis.analyse(omrData, methodData, path.toFile.getParentFile))
//      }
//      catch {
//        case e: Throwable => Left(ErrorData(e))
//      }
//    }

    /*def generation(path: SafePath, generation: Option[Long], all: Boolean) = {
      import ServerFileSystemContext.project
      import services._

      try {
        val (omrData, methodData) = Analysis.loadMetadata(path.toFile)
        Right(Analysis.generation(omrData, methodData, path.toFile.getParentFile, generation = generation, all = all))
      }
      catch {
        case e: Throwable => Left(ErrorData(e))
      }
    }*/



