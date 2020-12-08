package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.hook.omr.OMROutputFormat
import io.circe._
import io.circe.parser._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.outputmanager.OutputManager
import org.openmole.plugin.method.evolution.data._

object Analysis {

  import MetadataGeneration._

  def loadMetadata(file: File) = {
    val omrData = OMROutputFormat.omrData(file)
    decode[EvolutionMetadata](file.content(gz = true)) match {
      case Right(v) ⇒ (omrData, v)
      case Left(e)  ⇒ throw new InternalProcessingError(s"Error parsing ${file}", e)
    }
  }

  def dataFiles(directory: File, fileName: String, generation: Long, frequency: Option[Long])(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
    (0L to generation by frequency.getOrElse(1L)).drop(1).map { g ⇒
      val fileNameValue = (fileName: FromContext[String]).from(Context(GAIntegration.generationPrototype -> g))
      directory / fileNameValue
    }.filter(_.exists)
  }

  object EvolutionAnalysis {
    object none extends EvolutionAnalysis
  }

  sealed trait EvolutionAnalysis

  def analyse(omrData: OMROutputFormat.OMRData, metaData: EvolutionMetadata, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) =
    metaData match {
      case m: EvolutionMetadata.StochasticNSGA2 ⇒ Analysis.StochasticNSGA2.analyse(omrData, m, directory)
      case EvolutionMetadata.none               ⇒ ???
    }

  object StochasticNSGA2 {
    case class SavedGeneration(generation: Long, objectives: Vector[SavedObjective])
    case class SavedObjective(objectives: Vector[Double], samples: Int)

    import AnalysisData.StochasticNSGA2._

    def analyse(omrData: OMROutputFormat.OMRData, metaData: EvolutionMetadata.StochasticNSGA2, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {

      def generations =
        Analysis.dataFiles(directory, omrData.fileName, metaData.generation, metaData.saveOption.frequency).map { f ⇒
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
