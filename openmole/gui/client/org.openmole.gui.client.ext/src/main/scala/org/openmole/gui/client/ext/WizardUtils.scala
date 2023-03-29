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
package org.openmole.gui.client.ext

import org.openmole.gui.shared.data.*

object WizardUtils:
  def preamble =
    """/*
    |This is skeleton to plug your model into OpenMOLE. To go further you should complete it.
    |Define some hook, some exploration methods, and optionally some execution environment.
    |To do all that please refer to the OpenMOLE documentation.
    |*/""".stripMargin

  case class WizardModelData(
    vals: String,
    inputs: String,
    outputs: String,
    inputFileMapping: String,
    outputFileMapping: String,
    defaults: String,
    //   resources: String,
    specificInputMapping: Option[String] = None,
    specificOutputMapping: Option[String] = None)

  def wizardModelData(
    inputs: Seq[PrototypePair],
    outputs: Seq[PrototypePair],
    // resources: Seq[String],
    specificInputPattern: Option[String] = None,
    specificOutputPattern: Option[String] = None,
    ) = {

    def testBoolean(protoType: PrototypePair) = protoType.`type` match
      case PrototypeData.Boolean ⇒ if (protoType.default == "1") "true" else "false"
      case _ ⇒ protoType.default

    def ioString(protos: Seq[PrototypePair], keyString: String) = if (protos.nonEmpty) Seq(s"    $keyString += (", ")").mkString(protos.map { i ⇒ s"${i.name}" }.mkString(", ")) + ",\n" else ""
    def imapString(protos: Seq[PrototypePair], keyString: String) = if (protos.nonEmpty) protos.map { i ⇒ s"""    $keyString += ${i.name} mapped "${i.mapping.get}"""" }.mkString(",\n") + ",\n" else ""
    def omapString(protos: Seq[PrototypePair], keyString: String) = if (protos.nonEmpty) protos.map { o ⇒ s"""    $keyString += ${o.name} mapped "${o.mapping.get}"""" }.mkString(",\n") + ",\n" else ""

    def default(key: String, value: String) = s"    $key := $value"

    val (rawimappings, ins) = inputs.partition(i ⇒ i.mapping.isDefined)
    val (rawomappings, ous) = outputs.partition(o ⇒ o.mapping.isDefined)
    val (ifilemappings, imappings) = rawimappings.partition(_.`type` == PrototypeData.File)
    val (ofilemappings, omappings) = rawomappings.partition(_.`type` == PrototypeData.File)

    //val resourcesString = if (!resources.isEmpty) s"""  resources += (${resources.map { r ⇒ s"workDirectory / $r" }.mkString(",")})\n""" else ""

    val defaultValues =
      (inputs.map { p ⇒ (p.name, testBoolean(p)) } ++
        ifilemappings.map { p ⇒ (p.name, " workDirectory / \"" + p.mapping.getOrElse("") + "\"") }).filterNot { _._2.isEmpty }.map { p ⇒ default(p._1, p._2) }.mkString(",\n")

    val defaults = defaultValues

    val vals =
      ((inputs ++ outputs).map { p ⇒ (p.name, p.`type`.scalaString) } distinct).map { p =>
        "val " + p._1 + " = Val[" + p._2 + "]"
      }.mkString("\n")

    WizardModelData(
      vals,
      ioString(ins, "inputs"),
      ioString(ous, "outputs"),
      imapString(ifilemappings, "inputFiles"),
      omapString(ofilemappings, "outputFiles"),
      defaults,
   //   resourcesString,
      specificInputPattern.map { sip => imapString(imappings, sip) },
      specificOutputPattern.map { sop => omapString(omappings, sop) }
    )
  }

  def expandWizardData(modelData: WizardModelData) =
      modelData.inputs +
      modelData.outputs +
      modelData.specificInputMapping.getOrElse("") +
      modelData.specificOutputMapping.getOrElse("") +
      modelData.inputFileMapping +
      modelData.outputFileMapping +
    //  modelData.resources +
      modelData.defaults

  def mkTaskParameters(s: String*) = s.filter(!_.trim.isEmpty).map(s => s"    $s").mkString(",\n")

  def mkSet(s: String*) =
    val elements = s.filter(!_.trim.isEmpty).map(s => s"    $s").mkString(",\n")
    if elements.isEmpty
    then ""
    else
      s"""set (
         |$elements
         |  )""".stripMargin


  def singleFolderContaining(files: Seq[(RelativePath, SafePath)], f: ((RelativePath, SafePath)) => Boolean): Option[(RelativePath, SafePath)] =
    val firstLevel = files.map(f => f._1.value.take(1)).distinct
    if firstLevel.size == 1
    then files.filter(f => f._1.value.size == 2).find(f)
    else None

  def toTaskName(r: RelativePath) =
    def lowerCase(s: String) = s.take(1).toLowerCase ++ s.drop(1)
    lowerCase(r.nameWithoutExtension) + "Task"

  def toOMSName(r: RelativePath) =
    r.nameWithoutExtension.capitalize + ".oms"

  def toVariableName(s: String) =
    val capital: String = s.split('-').reduce(_ + _.capitalize)
    capital.replace("?", "").replace(" ", "").replace("%", "percent")



