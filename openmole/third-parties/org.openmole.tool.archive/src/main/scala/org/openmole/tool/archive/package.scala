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

import org.apache.commons.compress.archivers.tar

import java.io.*
import java.nio.file.*
import org.openmole.tool.file.*
import org.openmole.tool.stream.*
import org.tukaani.xz.{LZMA2Options, XZInputStream, XZOutputStream}
import org.apache.commons.compress.archivers.tar.*

import java.util.zip.{ZipFile, ZipInputStream}
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Stack}
import scala.io.{BufferedSource, Codec}
import scala.jdk.CollectionConverters.*


object Zip:

  def unzipStream(is: InputStream, to: File,  overwrite: Boolean = false) =
    to.mkdirs
    val zip = new ZipInputStream(is)
    try
      Iterator.continually(zip.getNextEntry).takeWhile(_ != null).foreach: entry =>
        val toFile = new File(to, entry.getName)
        if !overwrite && toFile.exists() then throw IOException(s"File $toFile already exists and overwrite is set to false")

        if entry.isDirectory
        then
          if !toFile.exists then toFile.mkdirs()
        else
          toFile.getParentFile.mkdir()
          toFile.withOutputStream(zip.transferTo)
    finally zip.close()

  // Extract .zip archive
  def unzip(from: File, to: File, overwrite: Boolean = false) = from.withInputStream: is =>
    unzipStream(is, to, overwrite)

  def zipEntries(file: File): Seq[ArchiveEntry] =
    val zip = new ZipFile(file)
    try zip.entries().asScala.map { e => ArchiveEntry(e.getName.split("/"), directory = e.isDirectory) }.toSeq
    finally zip.close()


object XZ:
  def extract(file: File, to: File) =
    to.withFileOutputStream: outputStream =>
      val inputStream = new FileInputStream(file)
      val inxz = XZ.inputStream(file)
      try
        val buffer = new Array[Byte](inputStream.available)
        Iterator.continually(inxz.read(buffer)).takeWhile(_ != -1).foreach { outputStream.write(buffer, 0, _) }
      finally inxz.close

  def inputStream(file: File): InputStream =
    val inputStream = new FileInputStream(file)
    new XZInputStream(inputStream, 100 * 1024)

  def compress(file: File, to: File) =
    val outfile = new FileOutputStream(to)
    val outxz = new XZOutputStream(outfile, new LZMA2Options(8), org.tukaani.xz.XZ.CHECK_SHA256)

    val infile = new FileInputStream(file)
    val buffer = new Array[Byte](8192)

    Iterator.continually(infile.read(buffer)).takeWhile(_ != -1).foreach { size => outxz.write(buffer, 0, size) }

    outxz.finish


case class ArchiveEntry(path: Seq[String], directory: Boolean)

export org.apache.commons.compress.archivers.tar.{TarArchiveOutputStream, TarArchiveInputStream}

def TarArchiveOutputStream(os: OutputStream, blockSize: Option[Int] = None) =
  val tos =
    blockSize match
      case None => new TarArchiveOutputStream(os)
      case Some(b) => new TarArchiveOutputStream(os, b)
  tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)
  tos

def TarArchiveInputStream(os: InputStream) =
  new TarArchiveInputStream(os)


enum ArchiveType:
  case Tar, TarGZ, TarXZ, Zip

extension(file: File)
  def listArchive(archive: ArchiveType): Seq[ArchiveEntry] =
    def tarEntryToArchiveEntry(e: TarArchiveEntry) = ArchiveEntry(e.getName.split('/'), e.isDirectory)
    archive match
      case ArchiveType.Tar => withClosable(TarArchiveInputStream(file.bufferedInputStream())) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.TarGZ => withClosable(TarArchiveInputStream(file.gzippedBufferedInputStream)) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.TarXZ => withClosable(TarArchiveInputStream(XZ.inputStream(file))) { _.entryIterator.map(tarEntryToArchiveEntry).toSeq }
      case ArchiveType.Zip => Zip.zipEntries(file)

  def archive(dest: File, time: Boolean = true, archive: ArchiveType.TarGZ.type | ArchiveType.Tar.type = ArchiveType.Tar) =
    archive match
      case ArchiveType.Tar => withClosable(TarArchiveOutputStream(dest.bufferedOutputStream())) { _.archive(file, time) }
      case ArchiveType.TarGZ => withClosable(TarArchiveOutputStream(dest.gzippedBufferedOutputStream)) { _.archive(file, time) }

  def extract(dest: File, overwrite: Boolean = false, archive: ArchiveType) =
    def extractTAR(dest: File, overwrite: Boolean = false) = withClosable(TarArchiveInputStream(file.bufferedInputStream())) { _.extract(dest, overwrite) }
    def extractUncompressTGZ(dest: File, overwrite: Boolean = false) = withClosable(TarArchiveInputStream(file.gzippedBufferedInputStream)) { _.extract(dest, overwrite) }
    def extractUncompressXZ(dest: File, overwrite: Boolean = false) = withClosable(TarArchiveInputStream(XZ.inputStream(file))) { _.extract(dest, overwrite) }

    archive match
        case ArchiveType.Tar => extractTAR(dest, overwrite = overwrite)
        case ArchiveType.TarGZ => extractUncompressTGZ(dest, overwrite = overwrite)
        case ArchiveType.TarXZ => extractUncompressXZ(dest, overwrite = overwrite)
        case ArchiveType.Zip => Zip.unzip(file, dest, overwrite = overwrite)

  def copyCompress(toF: File): File =
    if (toF.isDirectory) file.archive(toF, archive = ArchiveType.TarGZ)
    else file.copyCompressFile(toF)
    toF


implicit class TarOutputStreamDecorator(tos: TarArchiveOutputStream):
  def addFile(f: File, name: String) =
    f.withInputStream: is =>
      addStream(is, name, Files.size(f), f.mode)

  def addStream(is: InputStream, name: String, size: Long, mode: Int) =
    val entry = new TarArchiveEntry(name)
    entry.setSize(size)
    entry.setMode(mode)
    tos.putArchiveEntry(entry)
    try is.copy(tos)
    finally tos.closeArchiveEntry()

  def archive(directory: File, time: Boolean = true, includeTopDirectoryName: Boolean = false) =
    createDirArchiveWithRelativePathWithAdditionalCommand(
      tos,
      directory,
      if time then identity else _.setModTime(0),
      includeTopDirectoryName)

  // TODO detect hard links and tar them as so not as separated files
  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarArchiveOutputStream, directory: File, additionalCommand: TarArchiveEntry => Unit, includeDirectoryName: Boolean) =

    if (!Files.isDirectory(directory)) throw new IOException(directory.toString + " is not a directory.")

    val toArchive = new Stack[(File, String)]
    //val inodes = new mutable.HashMap[Long, String]()

    if (!includeDirectoryName) toArchive.push(directory -> "") else toArchive.push(directory -> directory.getName)

    while toArchive.nonEmpty
    do
      val (source, entryName) = toArchive.pop
      val isSymbolicLink = Files.isSymbolicLink(source)
      val isDirectory = Files.isDirectory(source)

      //val inode = source.inode

      // Hard links support has been disabled for now
      // it raises too many levels of symbolic links after extraction
      def archiveHardLink(target: String) =
        val e = new TarArchiveEntry(entryName, TarConstants.LF_LINK)
        val targetName = if target.startsWith("/") then target.drop(1) else target
        e.setLinkName(targetName)
        (e, false)

      def archiveDirectory =
        // walk the directory tree to add all its entries to stack
        source.withDirectoryStream(): stream =>
          for f ← stream.asScala
          do
            val newSource = source.resolve(f.getFileName)
            val newEntryName = entryName + '/' + f.getFileName
            toArchive.push((newSource, newEntryName))

        // create the actual tar entry for the directory
        val e = new TarArchiveEntry(entryName + '/', TarConstants.LF_DIR)
        (e, false)

      def archiveSymbolicLink =
        val e = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK)
        e.setLinkName(Files.readSymbolicLink(source).toString)
        (e, false)

      def archivePlainFile =
        val e = new TarArchiveEntry(entryName)
        e.setSize(Files.size(source))
        (e, true)

      val (e, copyInArchive) =
        if isDirectory && !isSymbolicLink
        then archiveDirectory
        else
          if isSymbolicLink
          then archiveSymbolicLink
          else archivePlainFile

      // complete current entry by fixing its modes and writing it to the archive
      if source != directory
      then
        if !isSymbolicLink then e.setMode(source.mode)
        e.setModTime(source.lastModified)
        additionalCommand(e)
        tos.putArchiveEntry(e)
        if copyInArchive then Files.copy(source, tos)
        tos.closeArchiveEntry()

      // Add source to inode map
      /*inode.foreach: in =>
        inodes += in -> entryName*/

implicit class TarInputStreamDecorator(tis: TarArchiveInputStream):
  def entryIterator: Iterator[TarArchiveEntry] =
    Iterator.continually(tis.getNextTarEntry).takeWhile(_ != null)

  def applyAndClose[T](f: TarArchiveEntry => T): Iterable[T] =
    try
      val ret = new ListBuffer[T]

      var e = tis.getNextTarEntry
      while (e != null)
      do
        ret += f(e)
        e = tis.getNextTarEntry
      ret
    finally tis.close

  def extract(directory: File, overwrite: Boolean = false) =
    if !directory.exists() then directory.mkdirs()
    if !Files.isDirectory(directory) then throw new IOException(directory.toString + " is not a directory.")

    /** set mode from an integer as retrieved from a Tar archive */
    def setMode(file: Path, m: Int) =
      val f = file.toRealPath().toFile
      f.setReadable((m & READ_MODE) != 0)
      f.setWritable((m & WRITE_MODE) != 0)
      f.setExecutable((m & EXEC_MODE) != 0)

    case class DirectoryMetaData(path: Path, mode: Int, time: Long)
    val directoryData = ListBuffer[DirectoryMetaData]()

    case class LinkData(dest: Path, linkName: String, hard: Boolean)
    val linkData = ListBuffer[LinkData]()

    Iterator.continually(tis.getNextTarEntry).takeWhile(_ != null).foreach: e =>
      val dest = Paths.get(directory.toString, e.getName)

      //if dest.toFile.getName.contains(".opq") then println("create " + dest)

      if e.isDirectory
      then
        Files.createDirectories(dest)
        directoryData += DirectoryMetaData(dest, e.getMode, e.getModTime.getTime)
      else
        Files.createDirectories(dest.getParent)

        // has the entry been marked as a symlink in the archive?
        if e.getLinkName.nonEmpty
        then linkData += LinkData(dest, e.getLinkName, e.isLink)
        // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
        else
          Files.copy(tis, dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ => overwrite } *)
          setMode(dest, e.getMode)

      dest.toFile.setLastModified(e.getModTime.getTime)


    // Process links
    for l <- linkData
      do
        if !l.hard
        then
          val link = Paths.get(l.linkName)
          try Files.createSymbolicLink(l.dest, link)
          catch
            case e: java.nio.file.FileAlreadyExistsException if overwrite =>
              l.dest.toFile.delete()
              Files.createSymbolicLink(l.dest, link)
        else
          val link = Paths.get(directory.toString, l.linkName)
          try Files.createLink(l.dest, link)
          catch
            case e: java.nio.file.FileAlreadyExistsException if overwrite =>
              l.dest.toFile.delete()
              Files.createLink(l.dest, link)
            case e: java.nio.file.FileSystemException =>
              Files.copy(link, l.dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ => overwrite } *)
              setMode(l.dest, link.toFile.mode)


    // Set directory right after extraction in case some directory are not writable
    for r ← directoryData
      do
        setMode(r.path, r.mode)
        r.path.toFile.setLastModified(r.time)




