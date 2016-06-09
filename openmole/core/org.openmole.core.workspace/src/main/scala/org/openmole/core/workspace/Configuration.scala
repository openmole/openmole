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

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.tool.file._
import org.apache.commons.configuration2._
import org.apache.commons.configuration2.builder._
import org.apache.commons.configuration2.builder.fluent._

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

  val config = new Configurations().properties(file)

  val reloading = new ReloadingFileBasedConfigurationBuilder(classOf[PropertiesConfiguration])
    .configure(new Parameters().fileBased().setFile(file))

  reloading.setAutoSave(true)

  def value(group: String, name: String): Option[String] =
    Option(config.getString(s"$group.$name"))

  def setValue(group: String, name: String, value: String) =
    config.setProperty(s"$group.$name", value)

  def setCommentedValue(group: String, name: String, value: String) =
    config.setProperty(s"# $group.$name", value)

  def clear() = {
    file.content = ""
  }

}

object ConfigurationString {

  implicit def stringConfigurationString: ConfigurationString[String] =
    new ConfigurationString[String] {
      def toString(t: String): String = t
    }

  implicit def intConfigurationString: ConfigurationString[Int] =
    new ConfigurationString[Int] {
      override def toString(t: Int): String = t.toString
    }

  implicit def finiteDurationConfigurationString: ConfigurationString[FiniteDuration] =
    new ConfigurationString[FiniteDuration] {
      override def toString(t: FiniteDuration): String = org.openmole.core.tools.service.stringFromDuration(t)
    }

  implicit def doubleConfigurationString: ConfigurationString[Double] =
    new ConfigurationString[Double] {
      def toString(t: Double): String = t.toString
    }

}

trait ConfigurationString[-T] {
  def toString(t: T): String
}

