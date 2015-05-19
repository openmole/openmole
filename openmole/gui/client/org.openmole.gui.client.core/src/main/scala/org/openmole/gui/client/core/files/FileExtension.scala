package org.openmole.gui.client.core.files

/*
 * Copyright (C) 11/05/15 // mathieu.leclaire@openmole.org
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

object FileExtension {

  sealed trait FileExtension {
    def extension: String
  }

  trait OpenMOLEScript

  case class DisplayableFile(extension: String, highlighter: String) extends FileExtension

  case class BinaryFile(extension: String) extends FileExtension

  val OMS = new DisplayableFile("oms", "scala") with OpenMOLEScript
  val SCALA = DisplayableFile("scala", "scala")
  val NETLOGO = DisplayableFile("nlogo", "nlogo")
  val SH = DisplayableFile("sh", "sh")
  val NO_EXTENSION = DisplayableFile("", "")
  val BINARY = BinaryFile("")

  def apply(treeNode: TreeNode) = {
    val last2 = treeNode.canonicalPath().split('.').takeRight(2)

    val fileName = last2.mkString(".")
    val fileType = last2.last match {
      case "oms"                   ⇒ OMS
      case "scala"                 ⇒ SCALA
      case "sh"                    ⇒ SH
      case "nlogo" | "csv" | "txt" ⇒ NO_EXTENSION
      case _                       ⇒ BINARY
    }

    (fileName, fileType)
  }
}