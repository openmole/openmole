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
import org.eclipse.jgit.api.errors._

class GitApiImpl(s: Services) extends GitAPI {

  implicit val services = s

  import s._

  implicit val context = org.openmole.gui.ext.data.ServerFileSystemContext.project

  def cloneGIT(url: String, folder: SafePath): Option[SafePath] = {

    val repositoryName = url.split('/').map {
      _.split('.')
    }.lastOption.flatMap {
      _.headOption
    }

    val targetFolder = repositoryName map { r ⇒ folder ++ r }

    targetFolder map { tf ⇒
      val ff: java.io.File = tf
      try {
        val git = Git.cloneRepository().setURI(url).setDirectory(ff).call()
      }
      catch {
        case ire: InvalidRemoteException ⇒ println("GIT remote Exception")
        case te: TransportException      ⇒ println("GIT transport Exception")
        case gae: GitAPIException        ⇒ println("GIT api Exception")
        case x: Any                      ⇒ println("Any: " + x)
      }
      tf
    }

  }

  def status(folder: SafePath, files: Seq[SafePath]) = {

    //get modified files from jgit
    //val mofifiedFiles = buildGitRepository(folder)

  }

  /*  def buildGitRepository(folder: SafePath): Seq[SafePath] = {
    val builder = new FileRepositoryBuilder()
    val repository = builder.setGitDir(folder).readEnvironment().findGitDir().build()
    val git = new Git(repository)

    val reader = git.getRepository().newObjectReader()

    val oldTreeIter = new CanonicalTreeParser
    val oldTree = git.getRepository.resolve("HEAD^{tree}")
    oldTreeIter.reset(reader, oldTree)

    val newTreeIter = new CanonicalTreeParser
    val newTree = git.getRepository.resolve("HEAD~1^{tree}")
    newTreeIter.reset(reader, newTree)

    val diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)
    diffFormatter.setRepository(git.getRepository)

    val entries = diffFormatter.scan(oldTreeIter, newTreeIter)

    import java.io._
    import scala.collection.JavaConversions._

    entries.map { e ⇒ GitFile(new File(e.getOldPath), SafePath(e.getNewPath, context), e.getChangeType.values()) }
  }*/

}