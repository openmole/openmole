/**
 * Created by Romain Reuillon on 05/05/16.
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
package org.openmole.tool

import java.io._
import java.nio.file._
import java.util.concurrent.{ Callable, ThreadPoolExecutor, TimeUnit, TimeoutException }
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }

import squants.time._
import reflect.Selectable.reflectiveSelectable

package object stream:

  val DefaultBufferSize = 16 * 1024

  def copy(inputStream: InputStream, outputStream: OutputStream) =
    val buffer = new Array[Byte](DefaultBufferSize)
    Iterator.continually(inputStream.read(buffer)).takeWhile(_ != -1).foreach {
      outputStream.write(buffer, 0, _)
    }

  def copy(inputStream: InputStream, outputStream: OutputStream, bufferSize: Int, timeout: Time)(implicit pool: ThreadPoolExecutor) = {
    val buffer = new Array[Byte](bufferSize)
    val reader = new ReaderRunnable(buffer, inputStream, bufferSize)

    Iterator.continually {
      val futureRead = pool.submit(reader)

      try futureRead.get(timeout.millis, TimeUnit.MILLISECONDS)
      catch {
        case (e: TimeoutException) ⇒
          futureRead.cancel(true)
          throw new IOException(s"Timeout on reading $bufferSize bytes, read was longer than $timeout ms.", e)
      }
    }.takeWhile(_ != -1).foreach {
      count ⇒
        val futureWrite = pool.submit(new WriterRunnable(buffer, outputStream, count))

        try futureWrite.get(timeout.millis, TimeUnit.MILLISECONDS)
        catch {
          case (e: TimeoutException) ⇒
            futureWrite.cancel(true)
            throw new IOException(s"Timeout on writing $count bytes, write was longer than $timeout ms.", e)
        }
    }
  }

  def NullPrintStream() = PrintStream(NullOutputStream())

  implicit class OutputStreamDecorator(os: OutputStream):
    def flushClose = {
      try os.flush
      finally os.close
    }

    def toGZ = new GZIPOutputStream(os)

    def append(content: String) = new PrintWriter(os).append(content).flush

    def appendLine(line: String) = append(line + "\n")


  implicit class InputStreamDecorator(is: InputStream):

    def toByteArray =
      val os = new ByteArrayOutputStream()
      try {
        copy(os)
        os.toByteArray
      }
      finally is.close()

    def copy(to: OutputStream): Unit = stream.copy(is, to)

    def copy(to: File, maxRead: Int, timeout: Time)(implicit pool: ThreadPoolExecutor): Unit =
      withClosable(new BufferedOutputStream(new FileOutputStream(to))) {
        copy(_, maxRead, timeout)
      }

    def copy(to: OutputStream, maxRead: Int, timeout: Time)(implicit pool: ThreadPoolExecutor) = stream.copy(is, to, maxRead, timeout)

    def toGZiped = new GZipedInputStream(is)

    def toGZ = new GZIPInputStream(is)

    // this one must have REPLACE_EXISTING enabled but does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
    def copy(file: File, replace: Boolean = true) =
      Files.copy(
        is,
        file.toPath,
        (if (replace) Seq(StandardCopyOption.REPLACE_EXISTING) else Seq()): _*
      )

    def mkString =
      try scala.io.Source.fromInputStream(is).mkString
      finally is.close()



  def withClosable[C <: { def close(): Unit }, T](open: ⇒ C)(f: C ⇒ T): T =
    val c = open
    try f(c)
    finally c.close()

  class ReaderRunnable(buffer: Array[Byte], from: InputStream, maxRead: Int) extends Callable[Int]:
    override def call: Int = from.read(buffer, 0, maxRead)

  class WriterRunnable(buffer: Array[Byte], to: OutputStream, amount: Int) extends Callable[Unit] :
    override def call: Unit =
      to.write(buffer, 0, amount)
      to.flush()


  def inputStreamSequence(h: InputStream, t: InputStream*): InputStream =
    t.foldLeft(h) { (c, n) => new SequenceInputStream(c, n) }
