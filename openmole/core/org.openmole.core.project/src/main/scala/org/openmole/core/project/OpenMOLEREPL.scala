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

import org.openmole.core.console.ScalaREPL
import org.openmole.core.dsl._
import org.openmole.core.workflow.tools._

object OpenMOLEREPL {

  def autoImports: Seq[String] = PluginInfo.pluginsInfo.toSeq.flatMap(_.namespaces).map(n ⇒ s"$n._")
  def keywordNamespace = "om"

  def keywordNamespaceCode =
    s"""
       |object $keywordNamespace extends ${classOf[DSLPackage].getCanonicalName} with ${PluginInfo.pluginsInfo.flatMap(_.keywordTraits).mkString(" with ")}
     """.stripMargin

  def dslImport = Seq("org.openmole.core.dsl._") ++ autoImports

  def initialisationCommands(imports: Seq[String]) =
    Seq(
      imports.map("import " + _).mkString("; "),
      keywordNamespaceCode
    )

  def newREPL(
    args: ConsoleVariables,
    quiet: Boolean = false,
    intialisation: ScalaREPL ⇒ Unit = _ ⇒ {},
    additionnalImports: Seq[String] = Seq.empty) = {

    def initialise(loop: ScalaREPL) = {
      args.workDirectory.mkdirs()
      loop.beQuietDuring {
        intialisation(loop)
        initialisationCommands(dslImport ++ additionnalImports).foreach {
          loop.interpret
        }
        ConsoleVariables.bindVariables(loop, args)
      }
      loop
    }

    initialise(new ScalaREPL(quiet = quiet))
  }
}
