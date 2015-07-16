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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.merge.MergeStrategy
import org.openmole.console._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.site.Config
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.logger.Logger
import org.openmole.tool.tar._

import scala.util.{ Success, Failure, Try }

object Market extends Logger {

  lazy val githubMarket =
    Repository(
      "https://github.com/openmole/openmole-market.git",
      Some(s ⇒ s"https://github.com/openmole/openmole-market/tree/master/$s")
    )

  object Tags {
    lazy val stochastic = Tag("Stochastic")
    lazy val simulation = Tag("Simulation")
    lazy val machineLearning = Tag("Machine Learning")
    lazy val R = Tag("R")
    lazy val data = Tag("Data")
    lazy val native = Tag("Native Code")
    lazy val netlogo = Tag("NetLogo")
    lazy val java = Tag("Java")
    lazy val ga = Tag("Genetic Algorithm")
    lazy val scala = Tag("Scala")
    lazy val plugin = Tag("Plugin")
    lazy val python = Tag("Python")
    lazy val calibration = Tag("Calibration")
  }

  case class Tag(label: String)
  case class Repository(url: String, viewURL: Option[String ⇒ String] = None)

  import Tags._

  case class MarketRepository(repository: Repository, entries: MarketEntry*)
  case class MarketEntry(name: String, directory: String, tags: Seq[Tag] = Seq.empty, directories: Seq[String] = Seq("")) {
    def files(baseDirectory: File) =
      for {
        dir ← directories
        f ← baseDirectory / dir listFilesSafe (_.endsWith(".oms"))
      } yield f
  }

  def entries = Seq(
    MarketRepository(githubMarket,
      MarketEntry("Pi Computation", "pi", Seq(stochastic, simulation)),
      MarketEntry("Random Forest", "randomforest", Seq(stochastic, machineLearning, native, data, python)),
      MarketEntry("Hello World in R", "R-hello", Seq(R, data, native)),
      MarketEntry("Fire in NetLogo", "fire", Seq(netlogo, stochastic, simulation)),
      MarketEntry("Hello World in Java", "java-hello", Seq(java)),
      MarketEntry("Calibration of Ants", "ants", Seq(netlogo, ga, simulation, calibration)),
      MarketEntry("Hello with OpenMOLE plugin", "hello-plugin", Seq(scala, java, plugin)),
      MarketEntry("SimpopLocal", "simpoplocal", Seq(stochastic, simulation, ga, scala, calibration))
    )
  )

}

import java.io.File

import Market._

case class DeployedMarketEntry(
  archive: String,
  entry: MarketEntry,
  readme: Option[String],
  codes: Seq[String],
  viewURL: Option[String])

class Market(repositories: Seq[MarketRepository], destination: File) {

  lazy val console = new Console()

  def archiveDirectoryName = "market"

  def generate(cloneDirectory: File, testScript: Boolean = true): Seq[DeployedMarketEntry] = {
    val archiveDirectory = destination / archiveDirectoryName
    archiveDirectory.mkdirs()
    for {
      marketRepository ← repositories
      repository = update(marketRepository, cloneDirectory)
      project ← marketRepository.entries
      if !testScript || test(repository, project, marketRepository.repository.url)
    } yield {
      val fileName = s"${project.name}.tgz".replace(" ", "_")
      val archive = archiveDirectory / fileName
      val projectDirectory = repository / project.directory
      projectDirectory archiveCompress archive
      val readme = projectDirectory / "README.md"

      DeployedMarketEntry(
        s"$archiveDirectoryName/$fileName",
        project,
        readme.contentOption,
        project.files(projectDirectory).map(_.content),
        marketRepository.repository.viewURL.map(_(project.directory))
      )
    }
  }

  def test(clone: File, project: MarketEntry, repository: String): Boolean =
    Try {
      PluginManager.synchronized {
        val projectDirectory = clone / project.directory
        val consoleProject = new Project(projectDirectory)
        val plugins = consoleProject.loadPlugins
        try {
          def exclusion = s"Project ${project} of repository $repository has been excluded "

          def compiles = for { file ← project.files(projectDirectory) } yield {
            consoleProject.compile(file, Seq.empty) match {
              case Compiled(puzzle) ⇒ true
              case CompilationError(e) ⇒
                Log.logger.log(Log.WARNING, exclusion + " because there was an error during compilation.", e)
                false
              case e ⇒
                Log.logger.log(Log.WARNING, exclusion + s" because the compilation raise the error $e")
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

  def update(repository: MarketRepository, cloneDirectory: File): File = {
    val directory = cloneDirectory / repository.repository.url.hash.toString

    directory / ".git" exists () match {
      case true ⇒
        val repo = Git.open(directory)
        val cmd = repo.pull()
        cmd.setStrategy(MergeStrategy.THEIRS)
        cmd.call()
      case false ⇒
        val command = Git.cloneRepository
        command.setDirectory(directory)
        command.setURI(repository.repository.url)
        command.call()
    }

    directory
  }
}

