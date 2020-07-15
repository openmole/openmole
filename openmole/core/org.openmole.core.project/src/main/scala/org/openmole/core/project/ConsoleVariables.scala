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

import org.openmole.core.console._
import org.openmole.core.services._
import org.openmole.tool.file._

object ConsoleVariables {

  def variablesName = "_variables_"
  def workDirectory = "workDirectory"

  def bindVariables(loop: ScalaREPL, variables: ConsoleVariables, variablesName: String = variablesName) =
    loop.beQuietDuring {
      loop.bind(variablesName, variables)
      loop.eval(s"""
        |import $variablesName._
        |import $variablesName.services._""".stripMargin)
    }

  def experimentName(f: File) = {
    val name = f.getName
    if (name.endsWith(".oms")) name.dropRight(".oms".length) else name
  }

  case class Experiment(name: String, launchTime: Long = System.currentTimeMillis()) {
    def launchDate = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date(launchTime))
  }

}

case class ConsoleVariables(
  args:          Seq[String],
  workDirectory: File,
  experiment:    ConsoleVariables.Experiment
)(
  implicit
  val services: Services
)
