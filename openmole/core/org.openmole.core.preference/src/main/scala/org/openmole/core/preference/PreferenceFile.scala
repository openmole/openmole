/**
 * Created by Romain Reuillon on 14/02/16.
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
package org.openmole.core.preference

import java.util.concurrent.locks.*
import org.openmole.tool.file.*
import org.apache.commons.configuration2.*
import org.apache.commons.configuration2.builder.*
import org.apache.commons.configuration2.builder.fluent.*
import org.openmole.tool.types.*
import org.openmole.tool.thread.*
import org.openmole.tool.lock.*
import squants.*
import squants.information.*

import java.io.StringWriter


object ConfigurationFile:

  def apply(file: File) =
    if (file.createNewFile) file.setPosixMode("rw-------")
    new ConfigurationFile(file)


class ConfigurationFile private (val file: File):

  @transient private lazy val lock = new ReentrantReadWriteLock()

  @transient private lazy val builder =
    val params = new Parameters
    new PropertiesConfiguration()
    new ReloadingFileBasedConfigurationBuilder(classOf[PropertiesConfiguration]).configure(params.fileBased().setFile(file))

  private def config = withThreadClassLoader(classOf[PropertiesConfiguration].getClassLoader) { builder.getConfiguration }

  def value(group: String, name: String): Option[String] = lock.read:
    Option(config.getString(s"$group.$name"))

  def setValue(group: String, name: String, value: String) = lock.write:
    config.setProperty(s"$group.$name", value)
    builder.save()

  def clearValue(group: String, name: String) = lock.write:
    config.clearProperty(s"$group.$name")
    builder.save()

  def clear(): Unit = lock.write:
    file.content = ""
    builder.resetResult()


object ConfigurationString:

  given ConfigurationString[String] with
    def toString(t: String): String = t
    def fromString(s: String) = implicitly[FromString[String]].apply(s)

  given ConfigurationString[java.util.UUID] with
    def toString(t: java.util.UUID): String = t.toString
    def fromString(s: String) = java.util.UUID.fromString(s)

  given ConfigurationString[Boolean] with
    def toString(t: Boolean): String = t.toString
     def fromString(s: String) = implicitly[FromString[Boolean]].apply(s)

  given ConfigurationString[Int] with
    override def toString(t: Int): String = t.toString
    override def fromString(s: String) = implicitly[FromString[Int]].apply(s)

  given ConfigurationString[Long] with
    override def toString(t: Long): String = t.toString
    override def fromString(s: String) = implicitly[FromString[Long]].apply(s)

  given ConfigurationString[Time] with
    override def toString(t: Time): String = t.toString
    override def fromString(s: String) = implicitly[FromString[Time]].apply(s)

  given ConfigurationString[Information] with
    override def toString(i: Information): String = i.toString
    override def fromString(s: String) = implicitly[FromString[Information]].apply(s)

  given ConfigurationString[Double] with
    def toString(t: Double): String = t.toString
    def fromString(s: String) = implicitly[FromString[Double]].apply(s)


  given [T](using cs: ConfigurationString[T]): ConfigurationString[Seq[T]] = new ConfigurationString[Seq[T]]:
    import au.com.bytecode.opencsv.{CSVParser, CSVWriter}

    override def toString(t: Seq[T]): String =
      val writer = new StringWriter()
      val csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER, "")
      csvWriter.writeNext(t.map(cs.toString).toArray)
      csvWriter.flush()
      writer.toString

    override def fromString(s: String): Seq[T] =
      if s.trim.isEmpty
      then Seq()
      else
        val parser = new CSVParser()
        parser.parseLine(s).toSeq.map(cs.fromString)


trait ConfigurationString[T]:
  def toString(t: T): String
  def fromString(s: String): T

