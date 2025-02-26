/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.marketindex

import java.io.File

import org.openmole.tool.file._
import org.openmole.tool.archive._
import org.openmole.core.market._

object Market:

  lazy val githubMarket =
    new Repository:
      def url = "https://github.com/openmole/openmole-market.git"
      def viewURL(name: String, branch: String) =
        Some(s"https://github.com/openmole/openmole-market/tree/$branch/$name")


//  object Tags:
//    lazy val abc = "ABC"
//    lazy val stochastic = "Stochastic"
//    lazy val simulation = "Simulation"
//    lazy val machineLearning = "Machine Learning"
//    lazy val R = "R"
//    lazy val julia = "Julia"
//    lazy val scilab = "Scilab"
//    lazy val fsl = "FSL"
//    lazy val neuroscience = "Neuro Science"
//    lazy val gama = "GAMA"
//    lazy val data = "Data"
//    lazy val native = "Native Code"
//    lazy val netlogo = "NetLogo"
//    lazy val java = "Java"
//    lazy val ga = "Genetic Algorithm"
//    lazy val scala = "Scala"
//    lazy val plugin = "Plugin"
//    lazy val python = "Python"
//    lazy val calibration = "Calibration"
//    lazy val optimisation =  "Optimisation"
//    lazy val diversity = "Diversity"
//    lazy val tutorial = "Tutorial"
//    lazy val workflow = "Workflow"
//    lazy val sensitivity = "Sensitivity"

  type Tag = String
  trait Repository:
    def url: String
    def viewURL(name: String, branch: String): Option[String]

//  import Tags._

  case class MarketRepository(repository: Repository, entries: MarketEntry*)
  case class MarketEntry(name: String, directory: String, tags: Seq[Tag] = Seq.empty)

//  def entries = Seq(
//    MarketRepository(
//      githubMarket,
//      MarketEntry("Hello World in Python", "python-hello", Seq(python)),
//      MarketEntry("Hello World in R", "R-hello", Seq(R)),
//      MarketEntry("Hello World in Scilab", "scilab-hello", Seq(scilab)),
//      MarketEntry("Hello World in Java", "hello-jvm", Seq(java)),
//      MarketEntry("Hello World in NetLogo", "fire", Seq(netlogo, stochastic, simulation)),
//      MarketEntry("Hello World in Julia", "julia-hello", Seq(julia)),
//      MarketEntry("Hello World in GAMA", "gama", Seq(gama, stochastic, simulation)),
//      MarketEntry("Hello World in a Container", "hello-container", Seq(native)),
//      MarketEntry("Hello with OpenMOLE plugin", "hello-plugin", Seq(scala, java, plugin)),
//
//      MarketEntry("Model Exploration Tutorial", "tutorials/method", Seq(netlogo, ga, simulation, calibration, tutorial, sensitivity, diversity)),
//      MarketEntry("Native Application Tutorial", "tutorials/native", Seq(tutorial, native, data, python)),
//      MarketEntry("Workflow Tutorial", "tutorials/workflow", Seq(tutorial, scala, workflow)),
//
//      MarketEntry("Morris Sensitivity Analysis", "sensitivity/morris", Seq(netlogo, sensitivity, simulation)),
//      MarketEntry("Saltelli Sensitivity Analysis", "sensitivity/saltelli", Seq(sensitivity, simulation)),
//      MarketEntry("ABC", "abc", Seq(abc, stochastic, calibration, tutorial)),
//      MarketEntry("Calibration of Ants", "ants", Seq(netlogo, ga, simulation, calibration)),
//      MarketEntry("Optimise Ackley function in Python", "ackley", Seq(python, ga, native, optimisation)),
//
//      MarketEntry("Pi Computation", "pi", Seq(stochastic, simulation, scala)),
//      MarketEntry("SimpopLocal", "simpoplocal", Seq(stochastic, simulation, ga, scala, calibration)),
//      MarketEntry("Metamimetic Networks", "metamimetic-networks", Seq(stochastic, simulation, netlogo)),
//      MarketEntry("Segmentation with FSL", "fsl-fast", Seq(fsl, data, native, neuroscience)),
//
//      MarketEntry("NSGA2 Test Functions", "nsga2-test-functions", Seq(calibration, optimisation, scala, ga)),
//      MarketEntry("Generate Visualisation", "genetic-algos-visu", Seq(ga, R)),
//
//      //MarketEntry("Random Forest", "randomforest", Seq(stochastic, machineLearning, native, data, python)),
//    )
//  )

  def generate(repositories: Seq[MarketRepository], destination: File, marketDirectory: File, branchName: String): Seq[GeneratedMarketEntry] =
    destination.mkdirs()
    for
      marketRepository ← repositories
      repository = marketRepository.repository
      project ← marketRepository.entries
    yield
      val fileName = s"${project.name}.tgz".replace(" ", "_")
      val archive = destination / fileName
      val projectDirectory = marketDirectory / project.directory
      projectDirectory archive (archive, archive = ArchiveType.TarGZ)

      GeneratedMarketEntry(
        fileName,
        project,
        projectDirectory,
        marketRepository.repository.viewURL(project.directory, branchName)
      )




import org.openmole.marketindex.Market._

case class GeneratedMarketEntry(
  archive:  String,
  entry:    MarketEntry,
  location: File,
  viewURL:  Option[String]
) {

  def readme = (location / "README.md").contentOption
  def tags = entry.tags

  def toDeployedMarketEntry =
    MarketIndexEntry(
      name = entry.name,
      archive = archive,
      readme = readme,
      tags = tags
    )
}

//class Market(repositories: Seq[MarketRepository], destination: File) {

// def test(directory: File, project: MarketEntry): Boolean = true
//DSLTest.withTmpServices { implicit servivces =>
//    Try {
//      PluginManager.synchronized {
//        val projectDirectory = directory / project.directory
//        val consoleProject = new Project(projectDirectory)
//        val plugins = consoleProject.loadPlugins
//        try {
//
//          def files = projectDirectory listRecursive (_.getName.endsWith(".oms"))
//
//          Log.logger.info(s"Test ${project.name} containing ${files.map(_.getName).mkString(",")}")
//
//          def exclusion = s"Project ${project} of repository $directory has been excluded "
//
//          def compiles = for { file ← files } yield {
//            consoleProject.compile(file, Seq.empty) match {
//              case Compiled(_) => true
//              case e: CompilationError =>
//                Log.logger.log(Log.WARNING, exclusion + s" because there was an error during compilation of file ${file.getName}.", e)
//                false
//              case e =>
//                Log.logger.log(Log.WARNING, exclusion + s" because the compilation of file ${file.getName} raise the error $e")
//                false
//            }
//          }
//
//          compiles.exists(_ != true)
//        }
//        finally {
//          plugins.foreach(_.uninstall())
//        }
//      }
//    } match {
//      case Failure(e) =>
//        Log.logger.log(Log.WARNING, s"Error durring $project test.", e)
//        false
//      case Success(_) => true
//    }
//  }

//}

