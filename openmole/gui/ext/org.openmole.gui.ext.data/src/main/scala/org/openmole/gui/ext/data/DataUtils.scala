package org.openmole.gui.ext.data

/*
 * Copyright (C) 05/01/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import FileExtension._

object DataUtils {

  case class IOArgs(args: Seq[VariableElement], ret: Option[VariableElement])

  private def typeStringToVariableElement(tString: String, index: Int): Option[VariableElement] =
    if (tString == "void") None
    else Some(VariableElement(index, PrototypePair(
      s"var$index",
      tString.toLowerCase match {
        case "double"           ⇒ PrototypeData.Double
        case "int"              ⇒ PrototypeData.Int
        case "java.io.File"     ⇒ PrototypeData.File
        case "boolean"          ⇒ PrototypeData.Boolean
        case "long"             ⇒ PrototypeData.Long
        case "float"            ⇒ PrototypeData.Double
        case "string" | "java.lang.String" ⇒ PrototypeData.String
        case "char"             ⇒ PrototypeData.Char
        case "short"            ⇒ PrototypeData.Short
        case "byte"             ⇒ PrototypeData.Byte
        case x: String          ⇒ PrototypeData.Any(x, x)
      }
    ), ScalaTaskType()))

  implicit private def typeStringsToScalaVariableElements(typeStrings: Seq[String]): Seq[VariableElement] =
    typeStrings.zipWithIndex.flatMap {
      case (ts, index) ⇒ typeStringToVariableElement(ts, index)
    }

  implicit def jarMethodToSeqVariableElements(jarMethod: JarMethod): IOArgs = {
    IOArgs(jarMethod.argumentTypes, typeStringToVariableElement(jarMethod.returnType, jarMethod.argumentTypes.size))
  }

  implicit class CleanName(s: String) {
    def clean = s.split('-').reduce(_ + _.capitalize).filterNot(Seq('?', ' ').contains).replaceAll("%", "percent")
  }

  def isCSV(safePath: SafePath) = {
    val name = safePath.name
    if (name.length > 4) name.takeRight(4) == ".csv"
    else false
  }

  def uuID = scala.util.Random.alphanumeric.take(10).mkString
}
