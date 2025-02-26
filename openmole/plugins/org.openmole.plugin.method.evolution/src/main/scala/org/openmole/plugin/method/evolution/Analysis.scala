//package org.openmole.plugin.method.evolution
//
//import org.openmole.core.dsl._
//import org.openmole.core.dsl.extension._
//import org.openmole.plugin.hook.omr.OMROutputFormat
//
//import io.circe._
//import io.circe.parser._
//import io.circe.generic.auto._
//import io.circe.generic.semiauto._
//
//import org.openmole.core.exception.InternalProcessingError
//import org.openmole.core.outputmanager.OutputManager
//
//object Analysis {
//
//  import MetadataGeneration._
//  import AnalysisData._
//
//  def loadMetadata(file: File) = {
//    val omrData = OMROutputFormat.indexData(file)
//    decode[EvolutionMetadata](file.content(gz = true)) match {
//      case Right(v) => (omrData, v)
//      case Left(e)  => throw new InternalProcessingError(s"Error parsing ${file}", e)
//    }
//  }
//
//  def dataFiles(directory: File, fileName: String, generation: Long, frequency: Option[Long])(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
//    (0L to generation by frequency.getOrElse(1L)).drop(1).map { (g: Long) => dataFile(directory, fileName, g) }.filter(_.exists)
//  }
//
//  def dataFile(directory: File, fileName: String, g: Long)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) =
//    directory / (fileName: FromContext[String]).from(Context(GAIntegration.generationVal -> g))
//
//  object EvolutionAnalysis {
//    object none extends EvolutionAnalysis
//  }
//
//  sealed trait EvolutionAnalysis
//
//  def analyse(omrData: OMROutputFormat.Index, metaData: EvolutionMetadata, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): AnalysisData.Convergence = {
//    metaData match {
//      case m: EvolutionMetadata.StochasticNSGA2 => Analysis.StochasticNSGA2.analyse(omrData, m, directory)
//      case m: EvolutionMetadata.NSGA2           => Analysis.NSGA2.analyse(omrData, m, directory)
//      case _                                    => ???
//    }
//  }
//
//  /*def generation(omrData: OMROutputFormat.OMRData, metaData: EvolutionMetadata, directory: File, generation: Option[Long] = None, all: Boolean = false)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): Seq[AnalysisData.Generation] = {
//    metaData match {
//      case m: EvolutionMetadata.StochasticNSGA2 => Analysis.StochasticNSGA2.generation(omrData, m, directory, generation = generation, all = all)
//      case m: EvolutionMetadata.NSGA2           => Analysis.NSGA2.generation(omrData, m, directory, generation = generation, all = all)
//      case _                                    => ???
//    }
//  }*/
//
//  object NSGA2 {
//    import AnalysisData.NSGA2._
//
////    def analyse(omrData: OMROutputFormat.Index, metaData: EvolutionMetadata.NSGA2, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
////      converge(allGenerations(omrData, metaData, directory).toVector)
////    }
//
////    def allGenerations(omrData: OMROutputFormat.Index, metaData: EvolutionMetadata.NSGA2, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) =
////      Analysis.dataFiles(directory, omrData.fileName, metaData.generation, metaData.saveOption.frequency).map(f => loadFile(metaData, f))
//
////    def generation(omrData: OMROutputFormat.OMRData, metaData: EvolutionMetadata.NSGA2, directory: File, generation: Option[Long], all: Boolean)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
////      (all, generation) match {
////        case (_, Some(g)) => Seq(loadFile(metaData, dataFile(directory, omrData.fileName, g)))
////        case (false, _)   => Seq(loadFile(metaData, dataFile(directory, omrData.fileName, metaData.generation)))
////        case (true, _)    => allGenerations(omrData, metaData, directory)
////      }
////    }
//
//    def loadFile(metaData: EvolutionMetadata.NSGA2, f: File) = {
//      val json = parse(f.content(gz = true)).right.get.asObject.get
//
//      def objectives: Vector[Vector[Double]] =
//        metaData.objective.toVector.map {
//          o => json(o.name).get.asArray.get.map(_.as[Double].toOption.get)
//        }.transpose
//
//      def genomes =
//        metaData.genome.toVector.map {
//          g =>
//            json(EvolutionMetadata.GenomeBoundData.name(g)).get.
//              asArray.get.
//              map(_.toString)
//        }
//
//      def savedObjectives = objectives map { o => Objective(o) }
//      def generation = json(GAIntegration.generationVal.name).get.asNumber.get.toLong.get
//
//      Generation(generation, genomes, savedObjectives)
//    }
//
//    def converge(generations: Vector[Generation]) = {
//      import _root_.mgo.tools.metric.Hypervolume
//
//      def robustObjectives(objectives: Vector[Objective]) = objectives.map(_.objectives.map(_.toDouble))
//
//      val nadir = {
//        val allRobustObjectives = generations.flatMap(g => robustObjectives(g.objective))
//        if (allRobustObjectives.isEmpty) None
//        else Some(Hypervolume.nadir(allRobustObjectives))
//      }
//
//      val generationsConvergence =
//        for {
//          generation ← generations.sortBy(_.generation)
//        } yield {
//          val rObj = robustObjectives(generation.objective)
//          def hv = nadir.flatMap { nadir => if (rObj.isEmpty) None else Some(Hypervolume(rObj, nadir)) }
//          def mins: Option[Vector[Double]] =
//            if (rObj.isEmpty)
//            then None
//            else
//              def transposed: Vector[Vector[Double]] = rObj.transpose
//              Some(transposed.map(_.min))
//
//          GenerationConvergence(generation.generation, hv, mins)
//        }
//
//      Convergence(nadir, generationsConvergence)
//    }
//
//  }
//
//  object StochasticNSGA2 {
//
//    import AnalysisData.StochasticNSGA2._
//
//    def converge(generations: Vector[Generation], samples: Int) = {
//      import _root_.mgo.tools.metric.Hypervolume
//
//      def robustObjectives(objectives: Vector[Objective]): Vector[Vector[Double]] =
//        objectives.filter(_.samples >= samples).map(_.objectives.map(_.toDouble))
//
//      val nadir = {
//        val allRobustObjectives = generations.flatMap(g => robustObjectives(g.objective))
//        if (allRobustObjectives.isEmpty) None
//        else Some(Hypervolume.nadir(allRobustObjectives))
//      }
//
//      val generationsConvergence =
//        for {
//          generation ← generations.sortBy(_.generation)
//        } yield {
//          val rObj = robustObjectives(generation.objective)
//          def hv = nadir.flatMap { nadir => if (rObj.isEmpty) None else Some(Hypervolume(rObj, nadir)) }
//          def mins: Option[Vector[Double]] =
//             if (rObj.isEmpty)
//             then None
//             else
//               def transposed: Vector[Vector[Double]] = rObj.transpose
//               Some(transposed.map(_.min))
//
//          GenerationConvergence(generation.generation, hv, mins)
//        }
//
//      Convergence(nadir, generationsConvergence)
//    }
//
//    /*def generation(omrData: OMROutputFormat.OMRData, metaData: EvolutionMetadata.StochasticNSGA2, directory: File, generation: Option[Long], all: Boolean)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
//      (all, generation) match {
//        case (_, Some(g)) => Seq(loadFile(metaData, dataFile(directory, omrData.ithaschangedfixit, g)))
//        case (false, _)   => Seq(loadFile(metaData, dataFile(directory, omrData.ithaschangedfixit, metaData.generation)))
//        case (true, _)    => allGenerations(omrData, metaData, directory)
//      }
//    }*/
//
//    def loadFile(metaData: EvolutionMetadata.StochasticNSGA2, f: File) = {
//      val json = parse(f.content(gz = true)).right.get.asObject.get
//
//      def objectives: Vector[Vector[Double]] =
//        metaData.objective.toVector.map {
//          o => json(o.name).get.asArray.get.map(_.as[Double].toOption.get)
//        }.transpose
//
//      def genomes =
//        metaData.genome.toVector.map {
//          g =>
//            json(EvolutionMetadata.GenomeBoundData.name(g)).get.
//              asArray.get.
//              map(_.toString)
//        }
//
//      def samples = json(GAIntegration.samplesVal.name).get.asArray.get.map(_.asNumber.get.toInt.get)
//      def savedObjectives = (objectives zip samples) map { case (o, s) => Objective(o, s) }
//      def generation = json(GAIntegration.generationVal.name).get.asNumber.get.toLong.get
//
//      Generation(generation, genomes, savedObjectives)
//    }
//
////    def allGenerations(omrData: OMROutputFormat.Index, metaData: EvolutionMetadata.StochasticNSGA2, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) =
////      Analysis.dataFiles(directory, omrData.fileName, metaData.generation, metaData.saveOption.frequency).map(f => loadFile(metaData, f))
//
//    def analyse(omrData: OMROutputFormat.Index, metaData: EvolutionMetadata.StochasticNSGA2, directory: File)(implicit randomProvider: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService) = {
//      converge(allGenerations(omrData, metaData, directory).toVector, metaData.sample)
//    }
//
//  }
//
//}
