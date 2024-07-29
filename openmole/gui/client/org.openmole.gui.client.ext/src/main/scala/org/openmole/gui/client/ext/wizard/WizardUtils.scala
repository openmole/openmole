/**
  * Created by Mathieu Leclaire on 19/04/18.
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
package org.openmole.gui.client.ext.wizard

import org.openmole.gui.shared.data.*

import scala.concurrent.Future

object WizardUtils:
  def preamble =
    """/*
    |This is skeleton to plug your model into OpenMOLE. To go further you should complete it.
    |Define some hook, some exploration methods, and optionally some execution environment.
    |To do all that please refer to the OpenMOLE documentation.
    |*/""".stripMargin


  def mkVals(modelMetadata: ModelMetadata, prototype: PrototypeData*) =
    def vals =
      ((modelMetadata.inputs ++ modelMetadata.outputs ++ prototype).map { p ⇒ (p.name, p.`type`.scalaString) } distinct).map: p =>
        s"val ${p._1} = Val[${p._2}]"

    vals.mkString("\n")

  def mkTaskParameters(s: String*) =
    val nonEmpty = s.filter(_.trim.nonEmpty)
    nonEmpty.headOption match
      case None => ""
      case Some(h) =>
        val all = Seq(h) ++ nonEmpty.drop(1).map(s => s"    $s")
        all.mkString(",\n")

  def mkSet(modelData: ModelMetadata, s: String*) =
    def setElements(inputs: Seq[PrototypeData], outputs: Seq[PrototypeData]) =
      def ioString(protos: Seq[PrototypeData], keyString: String) =
        if protos.nonEmpty
        then Seq(Seq(s"$keyString += (", ")").mkString(protos.map { i ⇒ s"${i.name}" }.mkString(", ")))
        else Seq()

      def imapString(protos: Seq[PrototypeData], keyString: String) =
        protos.flatMap: i ⇒
          i.mapping.map { mapping => s"""$keyString += ${i.name} mapped "${mapping}"""" }

      def omapString(protos: Seq[PrototypeData], keyString: String) =
        protos.flatMap: o ⇒
          o.mapping.map: mapping =>
            s"""$keyString += ${o.name} mapped "${mapping}""""

      def default(key: String, value: String) = s"$key := $value"

      val (rawimappings, ins) = inputs.partition(i ⇒ i.mapping.isDefined)
      val (rawomappings, ous) = outputs.partition(o ⇒ o.mapping.isDefined)
      val (ifilemappings, imappings) = rawimappings.partition(_.`type` == PrototypeData.File)
      val (ofilemappings, omappings) = rawomappings.partition(_.`type` == PrototypeData.File)

      //val resourcesString = if (!resources.isEmpty) s"""  resources += (${resources.map { r ⇒ s"workDirectory / $r" }.mkString(",")})\n""" else ""

      val defaultValues =
        (inputs.map { p ⇒ (p.name, p.default) } ++
          ifilemappings.map { p ⇒ (p.name, " workDirectory / \"" + p.mapping.getOrElse("") + "\"") }).filterNot {
          _._2.isEmpty
        }.map { p ⇒ default(p._1, p._2) }

      ioString(ins, "inputs") ++
        ioString(ous, "outputs") ++
        imapString(ifilemappings, "inputFiles") ++
        omapString(ofilemappings, "outputFiles") ++
        imapString(rawimappings, "inputs") ++
        omapString(rawomappings, "outputs") ++
        defaultValues

    end setElements

    val elements = (setElements(modelData.inputs, modelData.outputs) ++ s).filter(!_.trim.isEmpty).map(s => s"    $s").mkString(",\n")
    if elements.isEmpty
    then ""
    else
      s"""set (
         |$elements
         |  )""".stripMargin


  def mkCommandString(s: Seq[String]) =
    "Seq(" + s.map(s => s"\"$s\"").mkString(",") + ")"

  def findFileWithExtensions(files: Seq[(RelativePath, SafePath)], extensions: (String, FindLevel)*): Seq[AcceptedModel] =
    extensions.flatMap { (ext, level) =>
      level match
        case FindLevel.SingleFile => if files.size == 1 && files.head._1.value.size == 1 && files.head._1.name.endsWith(s".$ext") then Some(AcceptedModel(ext, FindLevel.SingleFile, List(files.head))) else None
        case FindLevel.MultipleFile =>
          val f = files.filter(_._1.value.size == 1).find(_._1.name.endsWith(s".$ext")).toList
          if f.nonEmpty then Some(AcceptedModel(ext, FindLevel.MultipleFile, f)) else None
        case FindLevel.Directory =>
          val f = singleFolderContaining(files, _._1.name.endsWith(s".$ext")).toList
          if f.nonEmpty then Some(AcceptedModel(ext, FindLevel.Directory, f)) else None
    }

  def singleFolderContaining(files: Seq[(RelativePath, SafePath)], f: ((RelativePath, SafePath)) => Boolean): Seq[(RelativePath, SafePath)] =
    val firstLevel = files.map(f => f._1.value.take(1)).distinct
    if firstLevel.size == 1
    then files.filter(f => f._1.value.size == 2).filter(f)
    else Seq()

  def quoted(s: String) = s"\"$s\""
  def tripleQuoted(s: String) = s"\"\"\"$s\"\"\""

  def inWorkDirectory(file: RelativePath | String) =
    file match
      case file: RelativePath => s"""workDirectory / ${quoted(file.mkString)}"""
      case s: String => s"""workDirectory / ${quoted(s)}"""

  private def lowerCase(s: String) = s.take(1).toLowerCase ++ s.drop(1).filter(c => c.isLetterOrDigit | c == '_')

  def toDirectoryName(r: RelativePath) = lowerCase(r.nameWithoutExtension)

  def toTaskName(r: RelativePath) = lowerCase(r.nameWithoutExtension) + "Task"

  def toOMSName(r: RelativePath) =
    r.nameWithoutExtension.capitalize.filter(c => c.isLetterOrDigit | c == '_') + ".oms"

  def toVariableName(s: String) =
    val capital: String = s.split('-').reduce(_ + _.capitalize)
    capital.replace("?", "").replace(" ", "").replace("%", "percent")

  def unknownError(acceptedModel: AcceptedModel, name: String) = Future.failed(new UnknownError(s"Unable to handle ${acceptedModel} in wizard $name"))



