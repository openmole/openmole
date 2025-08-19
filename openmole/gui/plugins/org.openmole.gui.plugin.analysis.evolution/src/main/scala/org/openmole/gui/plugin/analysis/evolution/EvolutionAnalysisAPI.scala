package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.plugin.method.evolution.AnalysisData

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

object EvolutionAnalysisAPI:
  //def analyse(path: SafePath): Either[ErrorData, AnalysisData.Convergence]
  lazy val analyse: TapirEndpoint[SafePath, AnalysisData.Convergence] =
    endpoint.post.in("evolution" / "analyse").in(jsonBody[SafePath]).out(jsonBody[AnalysisData.Convergence]).errorOut(jsonBody[ErrorData])

  //def generation(path: SafePath, generation: Option[Long] = None, all: Boolean = false): Either[ErrorData, Seq[AnalysisData.Generation]]
  //val generation: Endpoint[(SafePath, Option[Long], Boolean), Either[ErrorData, Seq[AnalysisData.Generation]]] =
  //  endpoint(post(path / "evolution" / "generation", jsonRequest[(SafePath, Option[Long], Boolean)]), ok(jsonResponse[Either[ErrorData, Seq[AnalysisData.Generation]]]))

