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
import org.openmole.console._
import org.openmole.core.tools.service.Logger
import org.openmole.site.Config
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.tar._

import scala.util.{ Success, Failure, Try }

object Market extends Logger {

  lazy val githubMarket = Repository("https://github.com/openmole/openmole-market.git")

  object Tags {
    lazy val stochastic = Tag("stochastic")
    lazy val simulation = Tag("simulation")
    lazy val machineLearning = Tag("machine learning")
  }

  case class Tag(label: String) extends AnyVal
  case class Repository(url: String) extends AnyVal

  import Tags._

  case class MarketRepository(url: String, entries: MarketEntry*)
  case class MarketEntry(name: String, directory: String, files: Seq[String], tags: Seq[Tag] = Seq.empty)

  def entries = Seq(
    MarketRepository("https://github.com/openmole/openmole-market.git",
      MarketEntry("pi", "pi", Seq("pi.oms"), Seq(stochastic, simulation)),
      MarketEntry("ramdomforest", "randomforest", Seq("learn.oms"), Seq(stochastic, machineLearning))
    )
  )

}

import java.io.File

import Market._

class Market(entries: Seq[MarketRepository], destination: File) {

  lazy val console = new Console()

  def archiveDirectoryName = "archives"
  def archiveDirectory = destination / archiveDirectoryName

  def generate(cloneDirectory: File, testScript: Boolean = true): Unit = {
    archiveDirectory.mkdirs()
    for {
      entry ← entries
      repository = update(entry, cloneDirectory)
      project ← entry.entries
      if !testScript || test(repository, project, entry.url)
    } {
      val fileName = s"${project.name}.tgz"
      val archive = archiveDirectory / fileName
      (repository / project.directory) archiveCompress archive
      for {
        tag ← project.tags
        link = (destination / tag.label / fileName)
      } {
        link.getParentFile.mkdirs()
        link createLink File("..") / archiveDirectoryName / fileName
      }
    }
  }

  def test(clone: File, project: MarketEntry, repository: String): Boolean = {
    def testScript(script: File): Try[Unit] = {
      def engine = console.newREPL(ConsoleVariables.empty)
      Try(engine.compiled(script.content))
    }

    project.files.forall {
      file ⇒
        testScript(clone / project.directory / file) match {
          case Failure(e) ⇒
            Log.logger.log(Log.WARNING, s"Project ${project} of repository $repository has been excluded", e)
            false
          case Success(_) ⇒ true
        }
    }
  }

  def update(repository: MarketRepository, cloneDirectory: File): File = {
    val directory = cloneDirectory / repository.url.hash.toString

    directory / ".git" exists () match {
      case true ⇒
        val repo = Git.open(directory)
        repo.pull()
      case false ⇒
        val command = Git.cloneRepository
        command.setDirectory(directory)
        command.setURI(repository.url)
        command.call()
    }

    directory
  }
}

