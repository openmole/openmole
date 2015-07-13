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
    lazy val scala = Tag("scala")
    lazy val plugin = Tag("plugin")
  }

  case class Tag(label: String)
  case class Repository(url: String, viewURL: Option[String ⇒ String] = None)

  import Tags._

  case class MarketRepository(repository: Repository, entries: MarketEntry*)
  case class MarketEntry(name: String, directory: String, files: Seq[String], tags: Seq[Tag] = Seq.empty)

  def entries = Seq(
    MarketRepository(githubMarket,
      MarketEntry("Pi Computation", "pi", Seq("pi.oms"), Seq(stochastic, simulation)),
      MarketEntry("Random Forest", "randomforest", Seq("learn.oms"), Seq(stochastic, machineLearning, native, data)),
      MarketEntry("Hello World in R", "R-hello", Seq("R.oms"), Seq(R, data, native)),
      MarketEntry("Fire in NetLogo", "fire", Seq("explore.oms"), Seq(netlogo, stochastic, simulation)),
      MarketEntry("Hello World in Java", "java-hello", Seq("explore.oms"), Seq(java)),
      MarketEntry("Calibration of Ants", "ants", Seq("calibrate.oms", "island.oms"), Seq(netlogo, ga, simulation)),
      MarketEntry("Hello with OpenMOLE plugin", "hello-plugin", Seq("hello.oms"), Seq(scala, java, plugin))
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
        project.files.map(f ⇒ projectDirectory / f content),
        marketRepository.repository.viewURL.map(_(project.directory))
      )
    }
  }

  def test(clone: File, project: MarketEntry, repository: String): Boolean =
    Try {
      PluginManager.synchronized {
        val projectDirectory = clone / project.directory
        val pluginsDirectory = projectDirectory / "plugins"
        val plugins = if (pluginsDirectory.exists()) PluginManager.load(pluginsDirectory.listFilesSafe) else List.empty
        try {
          def engine = console.newREPL(ConsoleVariables.empty)
          for { file ← project.files } engine.compiled(projectDirectory / file content)
          true
        }
        finally {
          plugins.foreach(_.uninstall())
        }
      }
    } match {
      case Success(b) ⇒ b
      case Failure(e) ⇒
        Log.logger.log(Log.WARNING, s"Project ${project} of repository $repository has been excluded", e)
        false
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

