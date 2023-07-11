/**
 * Created by Romain Reuillon on 25/05/16.
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
package org.openmole.core

package workflow {

  import org.openmole.core.context.ContextPackage
  import org.openmole.core.fromcontext._
  import org.openmole.core.keyword.KeyWordPackage
  import org.openmole.core.script.CodePackage
  import org.openmole.core.setter.BuilderPackage
  import org.openmole.core.workflow.mole.MolePackage
  import org.openmole.core.workflow.composition.CompositionPackage
  import org.openmole.core.workflow.hook.HookPackage
  import org.openmole.core.workflow.sampling.SamplingPackage
  import org.openmole.core.workflow.task.TaskPackage
  import org.openmole.core.workflow.tools.ToolsPackage
  import org.openmole.tool.types.TypesPackage

  trait ExportedPackage extends MolePackage
    with CompositionPackage
    with SamplingPackage
    with TaskPackage
    with ToolsPackage
    with BuilderPackage
    with TypesPackage
    with CodePackage
    with ContextPackage
    with ExpansionPackage
    with KeyWordPackage
    with HookPackage

  object dsl extends ExportedPackage

}
