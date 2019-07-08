/**
 * Created by Mathieu Leclaire on 19/04/18.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.gui.plugin.versioning.git

import org.openmole.core.services._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.server.Utils._
import org.eclipse.jgit.api.Git

import collection.JavaConverters._

class GitApiImpl(s: Services) extends GitAPI {

  implicit val services = s

  import s._

  implicit val context = org.openmole.gui.ext.data.ServerFileSystemContext.project

  private def git(repositoryPath: SafePath) = new Git(Git.open(repositoryPath).getRepository())

  private def remote(repositoryPath: SafePath) = {
    val remotes = git(repositoryPath).remoteList().call()
    if (remotes.isEmpty) None
    else Some(remotes.get(0).getURIs.get(0))
  }

  def clone(url: String, folder: SafePath): Option[MessageErrorData] = {

    val repositoryName = url.split('/').map {
      _.split('.')
    }.lastOption.flatMap {
      _.headOption
    }

    val targetFolder = repositoryName map { r ⇒ folder ++ r }

    println("repo name " + repositoryName)
    println("target folder " + targetFolder)

    scala.util.Try(
      targetFolder map { tf ⇒
        val ff: java.io.File = tf
        Git.cloneRepository().setURI(url).setDirectory(ff).call()
      }) match {
        case scala.util.Failure(e: Exception) ⇒
          println("Failure " + e.getStackTrace)
          Some(ErrorData(e))
        case _ ⇒ None
      }
  }

  def modifiedFiles(safePath: SafePath): Seq[SafePath] = {
    git(safePath).status.call().getModified.asScala.toSeq.map { f ⇒
      safePath.copy(path = safePath.path ++ f.split("/"))
    }
  }
}