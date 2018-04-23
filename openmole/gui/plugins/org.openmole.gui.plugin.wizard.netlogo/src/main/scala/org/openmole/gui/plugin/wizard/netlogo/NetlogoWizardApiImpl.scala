/**
 * Created by Mathieu Leclaire on 19/04/18.
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
package org.openmole.gui.plugin.wizard.netlogo

import org.openmole.core.services._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.DataUtils._
import org.openmole.gui.ext.tool.server.WizardUtils._
import org.openmole.gui.ext.tool.server.Utils
import org.openmole.gui.ext.tool.server.Utils._

class NetlogoWizardApiImpl(s: Services) extends NetlogoWizardAPI {

  def toTask(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources): SafePath = {

    val data = wizardModelData(inputs, outputs, resources, Some("netLogoInputs"), Some("netLogoOutputs"))
    val task = s"${executableName.split('.').head.toLowerCase}Task"

    val content = data.vals +
      s"""\nval launch = List("${(Seq("setup", "random-seed ${seed}") ++ (command.split('\n').toSeq)).mkString("\",\"")}")
            \nval $task = NetLogo6Task(workDirectory / ${executableName.split('/').map { s ⇒ s"""\"$s\"""" }.mkString(" / ")}, launch, embedWorkspace = ${!resources.implicits.isEmpty}) set(\n""".stripMargin +
      data.inputs + data.outputs + data.specificInputMapping.getOrElse("") + data.specificInputMapping.getOrElse("") + data.inputFileMapping + data.outputFileMapping + data.defaults +
      s"""\n\n$task hook ToStringHook()"""

    target.write(content)(context = org.openmole.gui.ext.data.ServerFileSystemContext.project, workspace = Workspace.instance)
    target
  }

  def parse(safePath: SafePath): Option[LaunchingCommand] = {

    val lines = Utils.lines(safePath)(context = org.openmole.gui.ext.data.ServerFileSystemContext.project, workspace = Workspace.instance)

    def parse0(lines: Seq[(String, Int)], args: Seq[ProtoTypePair], outputs: Seq[ProtoTypePair]): (Seq[ProtoTypePair], Seq[ProtoTypePair]) = {
      if (lines.isEmpty) (ProtoTypePair("seed", ProtoTYPE.INT, "0", None) +: args, outputs)
      else {
        val (line, index) = lines.head
        val tail = lines.tail
        if (line.startsWith("SLIDER")) parse0(tail, args :+ parseSlider(index), outputs)
        else if (line.startsWith("SWITCH")) parse0(tail, args :+ parseSwitch(index), outputs)
        else if (line.startsWith("INPUTBOX")) parse0(tail, args :+ parseInputBox(index), outputs)
        else if (line.startsWith("CHOOSER")) parse0(tail, args :+ parseChooser(index), outputs)
        else if (line.startsWith("MONITOR")) parse0(tail, args, outputs ++ parseMonitor(index))
        // else if (line.startsWith("PLOT")) parse0(tail, args, outputs ++ parsePlot(index))
        else parse0(tail, args, outputs)
      }
    }

    def parseSlider(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 9), Some(name))
    }

    def parseSwitch(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.BOOLEAN, lines(start + 7), Some(name))
    }

    def parseInputBox(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 6), Some(name))
    }

    def parseMonitor(start: Int): Seq[ProtoTypePair] = {
      val name = lines(start + 6).split(' ')
      if (name.size == 1) Seq(ProtoTypePair(name.head.clean, ProtoTYPE.DOUBLE, mapping = Some(name.head)))
      else Seq()
    }

    def parseChooser(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.STRING, lines(start + 7).split(' ').head, Some(name))
    }

    val (args, outputs) = parse0(lines.toSeq.zipWithIndex, Seq(), Seq())

    Some(BasicLaunchingCommand(
      Some(NetLogoLanguage()), "",
      args.distinct.zipWithIndex.map {
        case (a, i) ⇒ VariableElement(i, a, NetLogoTaskType())
      },
      outputs.distinct.zipWithIndex.map {
        case (o, i) ⇒ VariableElement(i, o, NetLogoTaskType())
      }
    ))
  }

}