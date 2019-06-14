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
    else Some(VariableElement(index, ProtoTypePair(
      s"var$index",
      tString match {
        case "double"           ⇒ ProtoTYPE.DOUBLE
        case "int"              ⇒ ProtoTYPE.INT
        case "java.io.File"     ⇒ ProtoTYPE.FILE
        case "boolean"          ⇒ ProtoTYPE.BOOLEAN
        case "long"             ⇒ ProtoTYPE.LONG
        case "float"            ⇒ ProtoTYPE.DOUBLE
        case "java.lang.String" ⇒ ProtoTYPE.STRING
        case "char"             ⇒ ProtoTYPE.CHAR
        case "short"            ⇒ ProtoTYPE.SHORT
        case "byte"             ⇒ ProtoTYPE.BYTE
        case x: String          ⇒ ProtoTYPE.ProtoTYPE(x, x, x)
      }
    ), ScalaTaskType()))

  implicit private def typeStringsToScalaVariableElements(typeStrings: Seq[String]): Seq[VariableElement] =
    typeStrings.zipWithIndex.flatMap {
      case (ts, index) ⇒ typeStringToVariableElement(ts, index)
    }

  implicit def jarMethodToSeqVariableElements(jarMethod: JarMethod): IOArgs = {
    IOArgs(jarMethod.argumentTypes, typeStringToVariableElement(jarMethod.returnType, jarMethod.argumentTypes.size))
  }

  implicit def fileToExtension(fileName: String): FileExtension = fileName match {
    case x if x.endsWith(".oms")                            ⇒ OMS
    case x if x.endsWith(".csv")                            ⇒ CSV
    case x if x.endsWith(".nlogo") | x.endsWith(".nlogo3d") ⇒ NETLOGO
    case x if x.endsWith(".R")                              ⇒ R
    case x if x.endsWith(".gaml") |
      x.endsWith(".py") |
      x.endsWith(".txt") | x.endsWith(".nls") ⇒ TEXT
    case x if x.endsWith(".md") ⇒ MD
    case x if x.endsWith(".tgz") | x.endsWith(".tar.gz") ⇒ TGZ
    case x if x.endsWith(".tar") ⇒ TAR
    case x if x.endsWith(".zip") ⇒ ZIP
    case x if x.endsWith(".tgz.bin") | x.endsWith(".tar.gz.bin") ⇒ TGZBIN
    case x if x.endsWith(".jar") ⇒ JAR
    case x if x.endsWith(".scala") ⇒ SCALA
    case x if x.endsWith(".sh") ⇒ SH
    case x if x.endsWith(".svg") ⇒ SVG
    case _ ⇒ BINARY
  }

  implicit class CleanName(s: String) {
    def clean = s.split('-').reduce(_ + _.capitalize).filterNot(Seq('?', ' ').contains).replaceAll("%", "percent")
  }

  def isCSV(safePath: SafePath) = {
    val name = safePath.name
    if (name.length > 4) name.takeRight(4) == ".csv"
    else false
  }
}
