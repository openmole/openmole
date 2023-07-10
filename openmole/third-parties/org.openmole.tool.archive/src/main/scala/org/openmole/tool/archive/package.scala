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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.tool.archive

import java.io.*
import java.nio.file.*
import org.openmole.tool.file.*
import org.openmole.tool.stream.*
import org.tukaani.xz.{LZMA2Options, XZInputStream, XZOutputStream}

import java.util.zip.ZipFile
import scala.collection.mutable.{ListBuffer, Stack}
import scala.io.{BufferedSource, Codec}
import scala.jdk.CollectionConverters.*


object Zip:

  // Extract .zip archive
  def unzip(from: File, to: File, overwrite: Boolean = false) =
    to.mkdirs
    val zip = new ZipFile(from)
    try
      for
        entry <- zip.entries.asScala
      do
        val toFile = new File(to, entry.getName)
        if !overwrite && toFile.exists() then throw IOException(s"File $toFile already exists and overwrite is set to false")
        if entry.isDirectory
        then if !toFile.exists then toFile.mkdirs
        else
          val is = new BufferedSource(zip.getInputStream(entry))(Codec.ISO8859)
          try toFile.withOutputStream { os => is foreach { (c: Char) ⇒ os.write(c) } }
          finally is.close()
    finally zip.close()


  def zipEntries(file: File): Seq[ArchiveEntry] =
    val zip = new ZipFile(file)
    try zip.entries().asScala.map { e => ArchiveEntry(e.getName.split("/"), directory = e.isDirectory) }.toSeq
    finally zip.close()


object XZ:
  def extract(file: File, to: File) =
    to.withFileOutputStream { outputStream =>
      val inputStream = new FileInputStream(file)
      val inxz = XZ.inputStream(file)
      try
        val buffer = new Array[Byte](inputStream.available)
        Iterator.continually(inxz.read(buffer)).takeWhile(_ != -1).foreach { outputStream.write(buffer, 0, _) }
      finally inxz.close
    }

  def inputStream(file: File): InputStream =
    val inputStream = new FileInputStream(file)
    new XZInputStream(inputStream, 100 * 1024)

  def compress(file: File, to: File) =
    val outfile = new FileOutputStream(to)
    val outxz = new XZOutputStream(outfile, new LZMA2Options(8), org.tukaani.xz.XZ.CHECK_SHA256)

    val infile = new FileInputStream(file)
    val buffer = new Array[Byte](8192)

    Iterator.continually(infile.read(buffer)).takeWhile(_ != -1).foreach { size ⇒ outxz.write(buffer, 0, size) }

    outxz.finish


case class ArchiveEntry(path: Seq[String], directory: Boolean)

enum ArchiveType:
  case Tar, TarGZ, TarXZ, Zip

extension(file: File)
  def listArchive(archive: ArchiveType): Seq[ArchiveEntry] =
    def tarEntryToArchiveEntry(e: TarEntry) = ArchiveEntry(e.getName.split('/'), e.isDirectory)
    archive match
      case ArchiveType.Tar => withClosable(new TarInputStream(file.bufferedInputStream())) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.TarGZ => withClosable(new TarInputStream(file.gzippedBufferedInputStream)) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.TarXZ => withClosable(new TarInputStream(XZ.inputStream(file))) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.Zip => Zip.zipEntries(file)

  def archive(dest: File, time: Boolean = true, archive: ArchiveType.TarGZ.type | ArchiveType.Tar.type = ArchiveType.Tar) =
    archive match
      case ArchiveType.Tar => withClosable(new TarOutputStream(dest.bufferedOutputStream())) { _.archive(file, time) }
      case ArchiveType.TarGZ => withClosable(new TarOutputStream(dest.gzippedBufferedOutputStream)) { _.archive(file, time) }

  def extract(dest: File, overwrite: Boolean = false, archive: ArchiveType) =
    def extractTAR(dest: File, overwrite: Boolean = false) = withClosable(new TarInputStream(file.bufferedInputStream())) { _.extract(dest, overwrite) }
    def extractUncompressTGZ(dest: File, overwrite: Boolean = false) = withClosable(new TarInputStream(file.gzippedBufferedInputStream)) { _.extract(dest, overwrite) }
    def extractUncompressXZ(dest: File, overwrite: Boolean = false) = withClosable(new TarInputStream(XZ.inputStream(file))) { _.extract(dest, overwrite) }

    archive match
        case ArchiveType.Tar => extractTAR(dest, overwrite = overwrite)
        case ArchiveType.TarGZ => extractUncompressTGZ(dest, overwrite = overwrite)
        case ArchiveType.TarXZ => extractUncompressXZ(dest, overwrite = overwrite)
        case ArchiveType.Zip => Zip.unzip(file, dest, overwrite = overwrite)

  def copyCompress(toF: File): File =
    if (toF.isDirectory) file.archive(toF, archive = ArchiveType.TarGZ)
    else file.copyCompressFile(toF)
    toF

  //def tarOutputStream = new TarOutputStream(file.bufferedOutputStream())



implicit class TarOutputStreamDecorator(tos: TarOutputStream):
  def addFile(f: File, name: String) =
    val entry = new TarEntry(name)
    entry.setSize(Files.size(f))
    entry.setMode(f.mode)
    tos.putNextEntry(entry)
    try Files.copy(f, tos) finally tos.closeEntry


  def archive(directory: File, time: Boolean = true, includeTopDirectoryName: Boolean = false) =
    createDirArchiveWithRelativePathWithAdditionalCommand(tos, directory, if (time) identity(_) else _.setModTime(0), includeTopDirectoryName)

  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarOutputStream, directory: File, additionalCommand: TarEntry ⇒ Unit, includeDirectoryName: Boolean) =

    if (!Files.isDirectory(directory)) throw new IOException(directory.toString + " is not a directory.")

    val toArchive = new Stack[(File, String)]
    if (!includeDirectoryName) toArchive.push(directory -> "") else toArchive.push(directory -> directory.getName)

    while (!toArchive.isEmpty) {

      val (source, entryName) = toArchive.pop
      val isSymbolicLink = Files.isSymbolicLink(source)
      val isDirectory = Files.isDirectory(source)

      // tar structure distinguishes symlinks
      val e =
        if (isDirectory && !isSymbolicLink) {
          // walk the directory tree to add all its entries to stack
          source.withDirectoryStream() { stream ⇒
            for (f ← stream.asScala) {
              val newSource = source.resolve(f.getFileName)
              val newEntryName = entryName + '/' + f.getFileName
              toArchive.push((newSource, newEntryName))
            }
          }
          // create the actual tar entry for the directory
          val e = new TarEntry(entryName + '/')
          e
        }
        // tar distinguishes symlinks
        else if (isSymbolicLink) {
          val e = new TarEntry(entryName, TarConstants.LF_SYMLINK)
          e.setLinkName(Files.readSymbolicLink(source).toString)
          e
        }
        // plain files
        else {
          val e = new TarEntry(entryName)
          e.setSize(Files.size(source))
          e
        }

      // complete current entry by fixing its modes and writing it to the archive
      if (source != directory) {
        if (!isSymbolicLink) e.setMode(source.mode)
        e.setModTime(source.lastModified)

        additionalCommand(e)
        tos.putNextEntry(e)

        if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) try Files.copy(source, tos)
        finally tos.closeEntry
      }
    }


implicit class TarInputStreamDecorator(tis: TarInputStream) {
  def entryIterator: Iterator[TarEntry] =
    Iterator.continually(tis.getNextEntry).takeWhile(_ != null)

  def applyAndClose[T](f: TarEntry ⇒ T): Iterable[T] = try {
    val ret = new ListBuffer[T]

    var e = tis.getNextEntry
    while (e != null) {
      ret += f(e)
      e = tis.getNextEntry
    }
    ret
  }
  finally tis.close

  def extract(directory: File, overwrite: Boolean = false) = {

    if (!directory.exists()) directory.mkdirs()
    if (!Files.isDirectory(directory)) throw new IOException(directory.toString + " is not a directory.")

    val directoryRights = ListBuffer[(Path, Int)]()

    Iterator.continually(tis.getNextEntry).takeWhile(_ != null).foreach {
      e ⇒
        val dest = Paths.get(directory.toString, e.getName)
        if (e.isDirectory) {
          Files.createDirectories(dest)
          directoryRights += (dest -> e.getMode)
        }
        else {
          Files.createDirectories(dest.getParent)

          // has the entry been marked as a symlink in the archive?
          if (!e.getLinkName.isEmpty) Files.createSymbolicLink(dest, Paths.get(e.getLinkName))
          // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
          else {
            Files.copy(tis, dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ ⇒ overwrite }: _*)
            dest.toFile.mode = e.getMode
          }
        }
        dest.setLastModified(e.getModTime)
    }

    // Set directory right after extraction in case some directory are not writable
    for {
      (path, mode) ← directoryRights
    } path.toFile.mode = mode

  }
}
