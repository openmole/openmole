/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.misc.tools.io
import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.io.FileWriter
import java.nio.file.FileSystems
import java.nio.file.Files
import org.junit.runner.RunWith
import scala.io.Source
import TarArchiver._
import FileUtil._


@RunWith(classOf[JUnitRunner])
class TarArchiverSpec extends FlatSpec with ShouldMatchers {
  "Archive" should "preserve symbolic links" in {
    val tmpDir = Files.createTempDirectory("testArch").toFile
    val file = new File(tmpDir, "file")
    file.createNewFile
    
    val fs = FileSystems.getDefault

    Files.createSymbolicLink(fs.getPath(tmpDir.getAbsolutePath, "link"), fs.getPath(file.getAbsolutePath))
   // Files.createSymbolicLink(fs.getPath(tmpDir.getAbsolutePath, "linkNoDest"), fs.getPath(file.getAbsolutePath))
    
    val archive = Files.createTempFile("archiveTest", ".tar").toFile

    archive.archiveDirWithRelativePathNoVariableContent(tmpDir)
    val extractDir = Files.createTempDirectory("testArchExtract").toFile
    archive.extractDirArchiveWithRelativePath(extractDir)
    
    Files.isSymbolicLink(fs.getPath(extractDir.getAbsolutePath, "link")) should equal (true)
    Files.isSameFile(fs.getPath(extractDir.getAbsolutePath, "link"), fs.getPath(extractDir.getAbsolutePath, "file")) should equal (true)
  
    extractDir.recursiveDelete
    archive.delete
    tmpDir.recursiveDelete
  }
}
