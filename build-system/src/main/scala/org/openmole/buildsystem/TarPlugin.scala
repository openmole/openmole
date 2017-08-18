package org.openmole.buildsystem

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveOutputStream }
import java.io.{ BufferedOutputStream, FileOutputStream }
import java.util.zip.GZIPOutputStream

import sbt._
import Keys._
import resource._
import scala.io.Source

object TarPlugin extends AutoPlugin {

  object autoImport {
    val tar = TaskKey[File]("tar", "Tar file produced by the assembly project")
    val tarInnerFolder = SettingKey[String]("tar-inner-folder", "All files in tar will be put under this folder")
    val tarName = SettingKey[String]("tar-name")
    val tarPath = SettingKey[File]("tar-path")
    val tarFolder = TaskKey[File]("tar-folder", "The folder to tar.")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    tarName := "assemble.tar.gz",
    tarPath := target.value / tarName.value,
    tarInnerFolder := "",
    tar := tarImpl(tarFolder.value, tarPath.value, target.value, tarInnerFolder.value, streams.value))

  def tarImpl(folder: File, tarFile: File, target: File, innerFolder: String, streams: TaskStreams): File = {
    val out = tarFile

    val tgzOS = managed {
      val tos = new TarArchiveOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(out))))
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      tos
    }

    def findFiles(f: File): Set[File] = if (f.isDirectory) (f.listFiles map findFiles flatten).toSet else Set(f)

    val files: Set[File] = findFiles(folder).toSet

    val fn = FileFunction.cached(target / "zip-cache", FilesInfo.lastModified, FilesInfo.exists) {
      fileSet ⇒
        streams.log.info("Zipping:\n\t")

        val lCP = folder

        for {
          os ← tgzOS
          file ← fileSet
          is ← managed(Source.fromFile(file)(scala.io.Codec.ISO8859))
        } {
          val relativeFile = innerFolder + "/" + (file relativeTo lCP).get.getPath
          streams.log.info("\t - " + relativeFile)

          val entry = new TarArchiveEntry(file, relativeFile)
          entry.setSize(file.length)
          if (file.canExecute) entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE | 111)

          os.putArchiveEntry(entry)

          for (c ← is.iter) {
            os.write(c.toByte)
          }

          os.closeArchiveEntry()
        }
        Set(out)
    }

    fn(files).head
  }

}