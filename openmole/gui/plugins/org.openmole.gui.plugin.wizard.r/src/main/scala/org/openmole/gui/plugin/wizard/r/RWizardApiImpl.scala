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
package org.openmole.gui.plugin.wizard.r

import org.openmole.core.services._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.DataUtils._
import org.openmole.gui.ext.tool.server.WizardUtils._
import org.openmole.gui.ext.tool.server.utils._
import org.openmole.tool.file._

class RWizardApiImpl(s: Services) extends RWizardAPI {

  import s._
  import org.openmole.gui.ext.data.ServerFileSystemContext.project

  def toTask(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources,
    data:           RWizardData): WizardToTask = {

    val modelData = wizardModelData(inputs, outputs, resources.all.map {
      _.safePath.name
    } :+ executableName, Some("inputs"), Some("ouputs"))

    val task = s"${executableName.split('.').head.toLowerCase}Task"

    val content = modelData.vals +
      s"""\nval $task = RTask(\"\"\"\n   source("$executableName")\n   \"\"\") set(\n""".stripMargin +
      expandWizardData(modelData) +
      s""")\n\n$task hook ToStringHook()"""

    target.toFile.content = content
    WizardToTask(target)
  }

  def parse(safePath: SafePath): Option[LaunchingCommand] = None

}