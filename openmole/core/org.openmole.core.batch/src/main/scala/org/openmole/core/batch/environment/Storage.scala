/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.batch.environment

import java.net.URI
import org.openmole.misc.tools.service.Logger
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.StorageControl
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.QualityControl
import org.openmole.core.batch.file.RelativePath
import org.openmole.core.batch.file.IURIFile

import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

object Storage extends Logger

abstract class Storage(val URI: URI) extends BatchService {

  @transient lazy val description = new ServiceDescription(URI)

  StorageControl.register(description, new QualityControl(Workspace.preferenceAsInt(BatchEnvironment.QualityHysteresis)))

  import Storage._

  @transient protected var baseSpaceVar: IURIFile = null

  def persistentSpace(token: AccessToken): IURIFile
  def tmpSpace(token: AccessToken): IURIFile
  def baseDir(token: AccessToken): IURIFile
  def root =
    if (URI.getScheme != null) new URI(URI.getScheme + "://" + URI.getAuthority)
    else new URI(URI.getScheme + ":/")
  def resolve(path: String) = root.resolve(path)

  def path = new RelativePath(root)

  /*def test: Boolean = {
    try {
      

      val token = StorageControl.usageControl(description).waitAToken

      try {
        val lenght = 10

        val rdm = new Array[Byte](lenght)

        RNG.nextBytes(rdm)

        val testFile = tmpSpace(token).newFileInDir("test", ".bin")
        val tmpFile = Workspace.newFile("test", ".bin")
        
        try {
          //BufferedWriter writter = new BufferedWriter(new FileWriter(tmpFile));
          val output = new FileOutputStream(tmpFile)
          try output.write(rdm)
          finally output.close

          URIFile.copy(tmpFile, testFile, token)
        } finally tmpFile.delete

        try {
          val local = testFile.cache(token)
          val input = new FileInputStream(local)
          val resRdm = new Array[Byte](lenght)
        
          val nb = try input.read(resRdm) finally input.close
          
          //String tmp = read.readLine();
          if (nb == lenght && rdm.deep == resRdm.deep) return true
          
        } finally ExecutorService.executorService(ExecutorType.REMOVE).submit(new URIFileCleaner(testFile, false))
      } finally StorageControl.usageControl(description).releaseToken(token)
    } catch {
      case e => logger.log(FINE, URI.toString, e)
    }
    return false
  }*/

  override def toString: String = URI.toString
}
