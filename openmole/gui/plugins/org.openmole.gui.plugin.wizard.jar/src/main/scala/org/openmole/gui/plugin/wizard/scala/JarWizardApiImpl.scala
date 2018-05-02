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
package org.openmole.gui.plugin.wizard.jar

import java.io.FileInputStream
import java.lang.reflect.Modifier
import java.util.zip.ZipInputStream

import org.openmole.core.services._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.server.Utils._
import org.openmole.gui.ext.tool.server.WizardUtils

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.tools.asm.tree

class JarWizardApiImpl(s: Services) extends JarWizardAPI {

  def toTask(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources,
    data:           JarWizardData): SafePath = {

    val modelData = WizardUtils.wizardModelData(inputs, outputs, resources.all.map {
      _.safePath.name
    })
    val task = s"${executableName.split('.').head.toLowerCase}Task"

    val jarResourceLine = {
      if (data.embedAsPlugin) {
        data.plugin.map { p ⇒
          s"""  plugins += pluginsOf(${p}),\n"""
        }.getOrElse("")
      }
      else s"${libraries.map { l ⇒ s"""  libraries += workingDirectory / "$l",""" }.getOrElse("")}\n\n"
    }

    val mainOutputString = outputs.headOption.map { o ⇒ s"val ${o.name} = " }.getOrElse("")

    val content = modelData.vals + s"""\n\nval $task = ScalaTask(\n\"\"\"$mainOutputString$command\"\"\") set(\n""" +
      jarResourceLine +
      WizardUtils.expandWizardData(modelData) +
      s""")\n\n$task hook ToStringHook()"""

    target.write(content)(context = org.openmole.gui.ext.data.ServerFileSystemContext.project, workspace = Workspace.instance)
    target
  }

  def parse(safePath: SafePath): Option[LaunchingCommand] =
    Some(BasicLaunchingCommand(Some(JavaLikeLanguage()), ""))

  def jarClasses(jarPath: SafePath): Seq[FullClass] = {

    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    implicit val workspace = Workspace.instance

    val zip = new ZipInputStream(new FileInputStream(jarPath))

    var classes: Seq[FullClass] = Seq()

    try {
      Stream.continually(zip.getNextEntry).takeWhile(_ != null).filter { e ⇒
        val name = e.getName
        name.endsWith(".class") &&
          !(name.startsWith("scala") || name.startsWith("java") || name.contains("""$anon$"""))
      }.foreach { e ⇒
        classes = classes :+ FullClass(e.getName.dropRight(6).replace('/', '.'))
      }
    }
    finally {
      zip.close
    }

    classes
  }

  def jarMethods(jarPath: SafePath, classString: String): Seq[JarMethod] = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    implicit val workspace = Workspace.instance

    val classLoader = new URLClassLoader(Seq(jarPath.toURI.toURL), this.getClass.getClassLoader)
    val clazz = Class.forName(classString, true, classLoader)

    clazz.getDeclaredMethods.map { m ⇒
      JarMethod(
        m.getName,
        m.getGenericParameterTypes.map {
          _.toString.split("class ").last
        }.toSeq,
        m.getReturnType.getCanonicalName, Modifier.isStatic(m.getModifiers), classString)
    }
  }

}