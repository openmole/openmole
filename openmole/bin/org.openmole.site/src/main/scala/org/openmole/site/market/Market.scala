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

package org.openmole.site.market

import org.openmole.core.project._
import org.openmole.core.buildinfo.MarketIndexEntry
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.site.Config
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.logger.Logger
import org.openmole.tool.tar._
import org.openmole.core.buildinfo
import collection.JavaConversions._

import scala.util.{ Success, Failure, Try }

object Market extends Logger {

  lazy val githubMarket =
    new Repository {
      def url = "https://github.com/openmole/openmole-market.git"
      def viewURL(name: String, branch: String) =
        Some(s"https://github.com/openmole/openmole-market/tree/$branch/$name")
    }

  object Tags {
    lazy val stochastic = Tag("Stochastic")
    lazy val simulation = Tag("Simulation")
    lazy val machineLearning = Tag("Machine Learning")
    lazy val R = Tag("R")
    lazy val fsl = Tag("FSL")
    lazy val neuroscience = Tag("Neuro Science")
    lazy val gama = Tag("GAMA")
    lazy val data = Tag("Data")
    lazy val native = Tag("Native Code")
    lazy val netlogo = Tag("NetLogo")
    lazy val java = Tag("Java")
    lazy val ga = Tag("Genetic Algorithm")
    lazy val scala = Tag("Scala")
    lazy val plugin = Tag("Plugin")
    lazy val python = Tag("Python")
    lazy val calibration = Tag("Calibration")
    lazy val tutorial = Tag("Tutorial")
  }

  case class Tag(label: String)
  trait Repository {
    def url: String
    def viewURL(name: String, branch: String): Option[String]
    def location(resourceDirectory: File) = resourceDirectory / "openmole-market"
  }

  import Tags._

  case class MarketRepository(repository: Repository, entries: MarketEntry*)
  case class MarketEntry(name: String, directory: String, tags: Seq[Tag] = Seq.empty)

  def entries = Seq(
    MarketRepository(
      githubMarket,
      MarketEntry("Pi Computation", "pi", Seq(stochastic, simulation, scala)),
      MarketEntry("Random Forest", "randomforest", Seq(stochastic, machineLearning, native, data, python)),
      MarketEntry("Hello World in R", "R-hello", Seq(R, data, native)),
      MarketEntry("Fire in NetLogo", "fire", Seq(netlogo, stochastic, simulation)),
      MarketEntry("Hello World in Java", "java-hello", Seq(java)),
      MarketEntry("Calibration of Ants", "ants", Seq(netlogo, ga, simulation, calibration)),
      MarketEntry("Hello with OpenMOLE plugin", "hello-plugin", Seq(scala, java, plugin)),
      MarketEntry("SimpopLocal", "simpoplocal", Seq(stochastic, simulation, ga, scala, calibration)),
      MarketEntry("Metamimetic Networks", "metamimetic-networks", Seq(stochastic, simulation, netlogo)),
      MarketEntry("Segmentation with FSL", "fsl-fast", Seq(fsl, data, native, neuroscience)),
      MarketEntry("Explore a GAMA Model", "gama", Seq(gama, stochastic, simulation)),
      MarketEntry("Introduction tutorial", "tutorials/introduction", Seq(tutorial, scala, stochastic)),
      MarketEntry("Native application tutorial", "tutorials/native", Seq(tutorial, native, data, python))
    )
  )

}

import java.io.File

import Market._

case class GeneratedMarketEntry(
    archive: String,
    entry:   MarketEntry,
    readme:  Option[String],
    viewURL: Option[String]
) {
  def toDeployedMarketEntry =
    MarketIndexEntry(
      name = entry.name,
      archive = archive,
      readme = readme,
      tags = entry.tags.map(_.label)
    )
}

class Market(repositories: Seq[MarketRepository], destination: File) {

  def branchName = buildinfo.version.takeWhile(_.isDigit) + "-dev"
  def archiveDirectoryName = "market"

  def generate(resourceDirectory: File, testScript: Boolean = true): Seq[GeneratedMarketEntry] = {
    val archiveDirectory = destination / archiveDirectoryName
    archiveDirectory.mkdirs()
    for {
      marketRepository ← repositories
      repository = marketRepository.repository
      project ← marketRepository.entries
      if !testScript || test(repository.location(resourceDirectory), project)
    } yield {
      val fileName = s"${project.name}.tgz".replace(" ", "_")
      val archive = archiveDirectory / fileName
      val projectDirectory = repository.location(resourceDirectory) / project.directory
      projectDirectory archiveCompress archive
      val readme = projectDirectory / "README.md"

      GeneratedMarketEntry(
        s"$archiveDirectoryName/$fileName",
        project,
        readme.contentOption,
        marketRepository.repository.viewURL(project.directory, branchName)
      )
    }
  }

  def test(directory: File, project: MarketEntry): Boolean =
    Try {
      PluginManager.synchronized {
        val projectDirectory = directory / project.directory
        val consoleProject = new Project(projectDirectory)
        val plugins = consoleProject.loadPlugins
        try {

          def files = projectDirectory listRecursive (_.getName.endsWith(".oms"))
          Log.logger.info(s"Test ${project.name} containing ${files.map(_.getName).mkString(",")}")

          def exclusion = s"Project ${project} of repository $directory has been excluded "

          def compiles = for { file ← files } yield {
            consoleProject.compile(file, Seq.empty) match {
              case Compiled(_) ⇒ true
              case e: CompilationError ⇒
                Log.logger.log(Log.WARNING, exclusion + s" because there was an error during compilation of file ${file.getName}.", e)
                false
              case e ⇒
                Log.logger.log(Log.WARNING, exclusion + s" because the compilation of file ${file.getName} raise the error $e")
                false
            }
          }
          compiles.exists(_ != true)
        }
        finally {
          plugins.foreach(_.uninstall())
        }
      }
    } match {
      case Failure(e) ⇒
        Log.logger.log(Log.WARNING, s"Error durring $project test.", e)
        false
      case Success(_) ⇒ true
    }

}

