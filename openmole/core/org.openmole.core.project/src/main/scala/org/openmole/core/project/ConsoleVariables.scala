/**
 * Created by Romain Reuillon on 22/01/16.
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
package org.openmole.core.project

import org.openmole.core.compiler._
import org.openmole.core.services._
import org.openmole.core.timeservice.TimeService
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.tool.file._

object ConsoleVariables:

  def variablesName = "__variables"
  def workDirectory = "workDirectory"

  def bindVariables(repl: REPL, variables: ConsoleVariables, variablesName: String = variablesName) =
    repl.bind(variablesName, variables)
    repl.eval(s"""
      |import $variablesName._
      |import $variablesName.services._""".stripMargin)
    

  def experimentName(f: File) =
    val name = f.getName
    if (name.endsWith(".oms")) name.dropRight(".oms".length) else name

  object Experiment:
    def apply(name: String)(implicit timeService: TimeService): Experiment = Experiment(name, timeService.currentTime)

  case class Experiment(name: String, launchTime: Long):
    def launchDate = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date(launchTime))


  def apply(
    args:                     Seq[String],
    workDirectory: File,
    experiment:    ConsoleVariables.Experiment)(implicit services: Services) =
    import services._
    new ConsoleVariables(args, workDirectory, experiment)(services, moleServices = MoleServices.create(services.tmpDirectory.directory))




case class ConsoleVariables private (
  args:                     Seq[String],
  @transient workDirectory: File,
  @transient experiment:    ConsoleVariables.Experiment
)(
  @transient implicit val services: Services,
  @transient implicit val moleServices: MoleServices
)
