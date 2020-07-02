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

  def dataFiles(directory: File, fileName: String, generation: Long, frequency: Option[Long]) = {
    (0L to generation by frequency.getOrElse(1L)).drop(1).map { g ⇒
      val fileNameValue = ExpandedString.expandValues(fileName, Context(GAIntegration.generationPrototype -> g))
      directory / OMROutputFormat.dataDirectory / s"$fileNameValue.json.gz"
    }.filter(_.exists)
  }

  object EvolutionAnalysis {
    object none extends EvolutionAnalysis
  }

  sealed trait EvolutionAnalysis

  def analyse(metaData: EvolutionMetadata, directory: File) = {
    metaData match {
      case m: EvolutionMetadata.StochasticNSGA2 ⇒ Analysis.StochasticNSGA2.analyse(m, directory)
      case EvolutionMetadata.none               ⇒ ???
    }
  }

  object StochasticNSGA2 {
    case class SavedGeneration(generation: Long, objectives: Vector[SavedObjective])
    case class SavedObjective(objectives: Vector[Double], samples: Int)

    import AnalysisData.StochasticNSGA2._

    def analyse(metaData: EvolutionMetadata.StochasticNSGA2, directory: File) = {

      def generations =
        Analysis.dataFiles(directory, metaData.saved.name, metaData.saved.generation, metaData.saved.frequency).map { f ⇒
          val json = parse(f.content(gz = true)).right.get.asObject.get

          def objectives: Vector[Vector[Double]] =
            metaData.objective.toVector.map {
              o ⇒ json(o.name).get.asArray.get.map(_.asNumber.get.toDouble)
            }.transpose

          def samples = json(GAIntegration.samples.name).get.asArray.get.map(_.asNumber.get.toInt.get)
          def savedObjectives = (objectives zip samples) map { case (o, s) ⇒ SavedObjective(o, s) }
          def generation = json(GAIntegration.generationPrototype.name).get.asNumber.get.toLong.get

          SavedGeneration(generation, savedObjectives)
        }

      converge(generations.toVector, metaData.sample)
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
