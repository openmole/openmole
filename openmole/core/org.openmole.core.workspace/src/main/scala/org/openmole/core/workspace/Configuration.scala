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
package org.openmole.core.workspace

import java.io.File

import org.openmole.tool.file._
import org.apache.commons.configuration2._
import org.apache.commons.configuration2.builder._
import org.apache.commons.configuration2.builder.fluent._
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer
import org.openmole.core.tools.io.FromString

import scala.concurrent.duration._

object ConfigurationLocation {

  def apply[T](group: String, name: String, default: ⇒ Option[T], cyphered: Boolean = false) =
    new ConfigurationLocation[T](group, name, default, cyphered)

}

class ConfigurationLocation[T](val group: String, val name: String, _default: ⇒ Option[T], val cyphered: Boolean) {
  def default = _default
  override def equals(obj: Any) = (group, name).equals(obj)
  override def hashCode = (group, name).hashCode
  override def toString = s"$group.$name"
}

class ConfigurationFile(val file: File) {

  @transient lazy val builder = {
    val params = new Parameters
    val builder =
      new ReloadingFileBasedConfigurationBuilder[FileBasedConfiguration](classOf[PropertiesConfiguration])
        .configure(params.fileBased()
          .setFile(file))
    builder.setAutoSave(true)
    builder
  }

  @transient lazy val config = {
    val c = builder.getConfiguration
    c.setSynchronizer(new ReadWriteSynchronizer())
    c
  }

  def value(group: String, name: String): Option[String] =
    Option(config.getString(s"$group.$name"))

  def setValue(group: String, name: String, value: String) =
    config.setProperty(s"$group.$name", value)

  def setCommentedValue(group: String, name: String, value: String) =
    config.setProperty(s"#$group.$name", value)

  def clear() = {
    file.content = ""
  }

}

object ConfigurationString {

  implicit def stringConfigurationString: ConfigurationString[String] =
    new ConfigurationString[String] {
      def toString(t: String): String = t
      def fromString(s: String) = implicitly[FromString[String]].apply(s)
    }

  implicit def intConfigurationString: ConfigurationString[Int] =
    new ConfigurationString[Int] {
      override def toString(t: Int): String = t.toString
      override def fromString(s: String) = implicitly[FromString[Int]].apply(s)
    }

  implicit def finiteDurationConfigurationString: ConfigurationString[FiniteDuration] =
    new ConfigurationString[FiniteDuration] {
      override def toString(t: FiniteDuration): String = org.openmole.core.tools.service.stringFromDuration(t)
      override def fromString(s: String) = implicitly[FromString[FiniteDuration]].apply(s)
    }

  implicit def doubleConfigurationString: ConfigurationString[Double] =
    new ConfigurationString[Double] {
      def toString(t: Double): String = t.toString
      def fromString(s: String) = implicitly[FromString[Double]].apply(s)
    }

  implicit def seqConfiguration[T](implicit cs: ConfigurationString[T]) = new ConfigurationString[Seq[T]] {
    override def toString(t: Seq[T]): String = t.map(cs.toString).mkString(",")
    override def fromString(s: String): Seq[T] = s.split(",").map(cs.fromString)
  }

}

trait ConfigurationString[T] {
  def toString(t: T): String
  def fromString(s: String): T
}

