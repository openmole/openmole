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

import org.openmole.core.exception.{ UserBadDataError, InternalProcessingError }
import org.openmole.tool.file._

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

  var lastModificationTime: Long = _
  var values: Map[String, Map[String, (Int, String)]] = _
  var commentedValues: Map[String, Map[String, (Int, String)]] = _

  load()

  def value(group: String, name: String): Option[String] = synchronized {
    checkModified
    val g = values.get(group)
    val v = g.flatMap(_.get(name).map(_._2))
    v
  }

  def commentedValue(group: String, name: String): Option[String] = synchronized {
    checkModified
    commentedValues.get(group).flatMap(_.get(name).map(_._2))
  }

  def setValue(group: String, name: String, value: String) = synchronized {
    checkModified
    values.get(group).flatMap(_.get(name)) match {
      case Some((i, l)) ⇒ replace(i, Value(group, name, value).toString)
      case None         ⇒ file.append(s"\n${Value(group, name, value)}")
    }
    load()
  }

  def addCommentedValue(group: String, name: String, value: String) = synchronized {
    checkModified
    file.append(s"\n# ${Value(group, name, value)}")
    load()
  }

  def removeValue(group: String, name: String) = synchronized {
    val lineNumbers = values(activeLines).map(_._1).toSet
    val content =
      for {
        (l, i) ← file.lines.zipWithIndex
        if !lineNumbers.contains(i)
      } yield l
    file.content = content.mkString("\n")
    load()
  }

  def clear() = synchronized {
    file.content = ""
    load()
  }

  private def replace(line: Int, content: String) = {
    val lines = file.lines
    val newLines = lines.take(line) ++ Seq(content) ++ lines.drop(line + 1)
    file.content = newLines.mkString("\n")
  }

  private def checkModified =
    if (file.lastModified() > lastModificationTime) load()

  private def load() = {
    lastModificationTime = file.lastModified()
    values = readValues
    commentedValues = readCommentedValues
  }

  private case class Value(group: String, name: String, value: String) {
    override def toString = s"${group}.${name} = ${value}"
  }

  private def readValues: Map[String, Map[String, (Int, String)]] = linesToMap(activeLines)

  private def readCommentedValues: Map[String, Map[String, (Int, String)]] = linesToMap(commentedLines)

  private def commentedLines =
    file.lines.zipWithIndex.map(_.swap).
      filter { case (_, l) ⇒ isCommentedLine(l) }.
      map { case (i, l) ⇒ i → l.trim.drop(1) }

  private def activeLines =
    file.lines.zipWithIndex.map(_.swap).
      filter { case (_, l) ⇒ !isCommentedLine(l) }

  private def parseValue(s: String) = {
    def split(s: String) = {
      val equalIndex = s.indexOf('=')
      if (equalIndex == -1) None
      else {
        val kp = s.take(equalIndex)
        val v = s.takeRight(s.length - equalIndex - 1)
        val Array(g, k) = kp.split('.')
        if (k.isEmpty) None
        else Some(Value(g.trim, k.trim, v.trim))
      }
    }
    split(s.trim)
  }

  private def isCommentedLine(s: String) =
    s.trim.headOption.map(_ == '#').getOrElse(false)

  private def linesToMap(lines: Seq[(Int, String)]) = toMap(values(lines))

  private def values(lines: Seq[(Int, String)]) = lines.flatMap { case (i, l) ⇒ parseValue(l).map(i → _) }

  private def toMap(vs: Seq[(Int, Value)]) = {
    def content =
      for {
        (g, values) ← vs.groupBy(_._2.group).toSeq
      } yield {
        def content = for {
          (n, inGroup) ← values.groupBy(_._2.name).toSeq
        } yield n → (inGroup.last._1 → inGroup.last._2.value)
        g → content.toMap
      }

    content.toMap
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

