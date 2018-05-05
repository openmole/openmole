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
package org.openmole.gui.ext.tool.server

import org.openmole.gui.ext.data._

object WizardUtils {

  def wizardModelData(inputs: Seq[ProtoTypePair],
                      outputs: Seq[ProtoTypePair],
                      resources: Seq[String],
                      specificInputPattern: Option[String] = None,
                      specificOutputPattern: Option[String] = None,
                     ) = {

    def testBoolean(protoType: ProtoTypePair) = protoType.`type` match {
      case ProtoTYPE.BOOLEAN ⇒ if (protoType.default == "1") "true" else "false"
      case _ ⇒ protoType.default
    }

    def ioString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) Seq(s"  $keyString += (", ")").mkString(protos.map { i ⇒ s"${i.name}" }.mkString(", ")) + ",\n" else ""

    def imapString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) protos.map { i ⇒ s"""  $keyString += (${i.name}, "${i.mapping.get}")""" }.mkString(",\n") + ",\n" else ""

    def omapString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) protos.map { o ⇒ s"""  $keyString += ("${o.mapping.get}", ${o.name})""" }.mkString(",\n") + ",\n" else ""

    def default(key: String, value: String) = s"  $key := $value"

    val (rawimappings, ins) = inputs.partition(i ⇒ i.mapping.isDefined)
    val (rawomappings, ous) = outputs.partition(o ⇒ o.mapping.isDefined)
    val (ifilemappings, imappings) = rawimappings.partition(_.`type` == ProtoTYPE.FILE)
    val (ofilemappings, omappings) = rawomappings.partition(_.`type` == ProtoTYPE.FILE)

    val resourcesString = if (!resources.isEmpty) s"""  resources += (${resources.map { r ⇒ s"workDirectory / $r" }.mkString(",")})\n""" else ""

    val defaults =
      "  //Default values. Can be removed if OpenMOLE Vals are set by values coming from the workflow\n" +
        (inputs.map { p ⇒ (p.name, testBoolean(p)) } ++
          ifilemappings.map { p ⇒ (p.name, " workDirectory / \"" + p.mapping.getOrElse("") + "\"") }).filterNot {
          _._2.isEmpty
        }.map { p ⇒ default(p._1, p._2) }.mkString(",\n")

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
      resourcesString,
      specificInputPattern.map { sip => imapString(imappings, sip) },
      specificOutputPattern.map { sop => imapString(omappings, sop) }
    )
  }

  def expandWizardData(modelData: WizardModelData) =
      modelData.inputs +
      modelData.outputs +
      modelData.specificInputMapping.getOrElse("") +
      modelData.specificInputMapping.getOrElse("") +
      modelData.inputFileMapping +
      modelData.outputFileMapping +
      modelData.resources +
      modelData.defaults
}
