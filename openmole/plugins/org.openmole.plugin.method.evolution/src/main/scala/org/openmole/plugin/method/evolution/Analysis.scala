package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.hook.omr.OMROutputFormat
import io.circe._
import io.circe.parser._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.plugin.method.evolution.data._

object Analysis {

  import MetadataGeneration._

  def loadMetadata(file: File): EvolutionMetadata = {
    decode[EvolutionMetadata](file.content(gz = true)) match {
      case Right(v) ⇒ v
      case Left(e)  ⇒ throw new InternalProcessingError(s"Error parsing ${file}", e)
    }
  }

  def dataFiles(directory: File, fileName: String, generation: Long, frequency: Option[Long]) =
    (0L to generation by frequency.getOrElse(1L)) map { g ⇒
      val fileNameValue = ExpandedString.expandValues(fileName, Context(GAIntegration.generationPrototype -> g))
      directory / OMROutputFormat.dataDirectory / fileNameValue
    }

  object EvolutionAnalysis {
    object none extends EvolutionAnalysis
  }

  sealed trait EvolutionAnalysis

  object StochasticNSGA2 {
    case class SavedGeneration(generation: Long, objectives: Vector[SavedObjective])
    case class SavedObjective(objectives: Vector[Double], samples: Int)

    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence])
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])

    def analyse(metaData: StochasticNSGA2Data, directory: File) = {
      println(Analysis.dataFiles(directory, metaData.saved.name, metaData.saved.generation, metaData.saved.frequency))
    }

    def converge(generations: Vector[SavedGeneration], samples: Int) = {
      import _root_.mgo.tools.metric.Hypervolume

      def robustObjectives(objectives: Vector[SavedObjective]) =
        objectives.filter(_.samples >= samples).map(_.objectives)

      val nadir = {
        val allRobustObjectives = generations.flatMap(g ⇒ robustObjectives(g.objectives))
        if (allRobustObjectives.isEmpty) None else Some(Hypervolume.nadir(allRobustObjectives))
      }

      val generationsConvergence =
        for {
          generation ← generations.sortBy(_.generation)
        } yield {
          val rObj = robustObjectives(generation.objectives)
          def hv = nadir.flatMap { nadir ⇒ if (rObj.isEmpty) None else Some(Hypervolume(rObj, nadir)) }
          def mins = if (rObj.isEmpty) None else Some(rObj.transpose.map(_.min))

          GenerationConvergence(generation.generation, hv, mins)
        }

      Convergence(nadir, generationsConvergence)
    }
  }

}
