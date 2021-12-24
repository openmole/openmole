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

import java.util.concurrent.locks._

import org.openmole.tool.file._
import org.apache.commons.configuration2._
import org.apache.commons.configuration2.builder._
import org.apache.commons.configuration2.builder.fluent._
import org.openmole.tool.types._
import org.openmole.tool.thread._
import org.openmole.tool.lock._
import squants._
import squants.information._


object ConfigurationFile {
  def apply(file: File) = {
    if (file.createNewFile) file.setPosixMode("rw-------")
    new ConfigurationFile(file)
  }

}

class ConfigurationFile private (val file: File) {

  @transient private lazy val lock = new ReentrantReadWriteLock()

  @transient private lazy val builder = {
    val params = new Parameters
    new PropertiesConfiguration()
    new ReloadingFileBasedConfigurationBuilder(classOf[PropertiesConfiguration]).configure(params.fileBased().setFile(file))
  }

  private def config = withThreadClassLoader(classOf[PropertiesConfiguration].getClassLoader) { builder.getConfiguration }

  def value(group: String, name: String): Option[String] = lock.read {
    Option(config.getString(s"$group.$name"))
  }

  def setValue(group: String, name: String, value: String) = lock.write {
    config.setProperty(s"$group.$name", value)
    builder.save()
  }

  def clearValue(group: String, name: String) = lock.write {
    config.clearProperty(s"$group.$name")
    builder.save()
  }

  def clear(): Unit = lock.write {
    file.content = ""
    builder.resetResult()
  }

}

object ConfigurationString {

  implicit def stringConfigurationString: ConfigurationString[String] =
    new ConfigurationString[String] {
      def toString(t: String): String = t
      def fromString(s: String) = implicitly[FromString[String]].apply(s)
    }

  implicit def booleanConfigurationString: ConfigurationString[Boolean] =
    new ConfigurationString[Boolean] {
      def toString(t: Boolean): String = t.toString
      def fromString(s: String) = implicitly[FromString[Boolean]].apply(s)
    }

  implicit def intConfigurationString: ConfigurationString[Int] =
    new ConfigurationString[Int] {
      override def toString(t: Int): String = t.toString
      override def fromString(s: String) = implicitly[FromString[Int]].apply(s)
    }

  implicit def LongConfigurationString: ConfigurationString[Long] =
    new ConfigurationString[Long] {
      override def toString(t: Long): String = t.toString
      override def fromString(s: String) = implicitly[FromString[Long]].apply(s)
    }

  implicit def timeConfigurationString: ConfigurationString[Time] =
    new ConfigurationString[Time] {
      override def toString(t: Time): String = t.toString
      override def fromString(s: String) = implicitly[FromString[Time]].apply(s)
    }

  implicit def informationConfigurationString: ConfigurationString[Information] =
    new ConfigurationString[Information] {
      override def toString(i: Information): String = i.toString
      override def fromString(s: String) = implicitly[FromString[Information]].apply(s)
    }

  implicit def doubleConfigurationString: ConfigurationString[Double] =
    new ConfigurationString[Double] {
      def toString(t: Double): String = t.toString
      def fromString(s: String) = implicitly[FromString[Double]].apply(s)
    }

  implicit def seqConfiguration[T](implicit cs: ConfigurationString[T]): ConfigurationString[Seq[T]] = new ConfigurationString[Seq[T]] {
    override def toString(t: Seq[T]): String = t.map(cs.toString).mkString(",")
    override def fromString(s: String): Seq[T] = s.split(",").toSeq.map(cs.fromString)
  }

}

trait ConfigurationString[T] {
  def toString(t: T): String
  def fromString(s: String): T
}

