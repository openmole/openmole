package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.shared.data.*
import org.openmole.plugin.method.evolution.data._

import org.openmole.gui.shared.data.*
import org.openmole.gui.ext.api.*

trait EvolutionAnalysisAPI extends RESTAPI {
  //def analyse(path: SafePath): Either[ErrorData, AnalysisData.Convergence]
  val analyse: Endpoint[SafePath, Either[ErrorData, AnalysisData.Convergence]] =
    endpoint(post(path / "evolution" / "analyse", jsonRequest[SafePath]), ok(jsonResponse[Either[ErrorData, AnalysisData.Convergence]]))

  //def generation(path: SafePath, generation: Option[Long] = None, all: Boolean = false): Either[ErrorData, Seq[AnalysisData.Generation]]
  //val generation: Endpoint[(SafePath, Option[Long], Boolean), Either[ErrorData, Seq[AnalysisData.Generation]]] =
  //  endpoint(post(path / "evolution" / "generation", jsonRequest[(SafePath, Option[Long], Boolean)]), ok(jsonResponse[Either[ErrorData, Seq[AnalysisData.Generation]]]))

}
