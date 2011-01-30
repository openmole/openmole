/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.ui.console.internal.command.initializer

import java.util.logging.Level
import java.util.logging.Logger
import org.codehaus.groovy.tools.shell.Shell
import org.openmole.commons.tools.obj.SuperClassesLister
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.InteractiveConfiguration
import org.openmole.ui.console.internal.Activator

class EnvironmentInitializer(shell: Shell) extends IInitializer {

  override def initialize(environment: Object, obj: Class[_]) = {
    //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    for (c <- SuperClassesLister.listSuperClasses(obj)) {

      //Look for companion object
      var toSearch = c;
      var obj: Object = null;
      try {
        toSearch = classOf[org.codehaus.groovy.tools.shell.Groovysh].getClassLoader.loadClass(c.getName + "$")
        obj = shell.execute(c.getName + "$.MODULE$")

        //if (toSearch != null) {
          for (f <- toSearch.getDeclaredFields) {
            val interactiveConfiguration = f.getAnnotation(classOf[InteractiveConfiguration])

            if (interactiveConfiguration != null) {
              if (classOf[ConfigurationLocation].isAssignableFrom(f.getType)) {
                val accessible = f.isAccessible
                f.setAccessible(true)
                val location = f.get(obj).asInstanceOf[ConfigurationLocation];
                f.setAccessible(accessible)

                val enabled = if (!interactiveConfiguration.dependOn().isEmpty()) {
                  val value = Activator.getWorkspace().preference(new ConfigurationLocation(location.group, interactiveConfiguration.dependOn))
                  value.equals(interactiveConfiguration.value());
                } else true

                if (enabled) {

                  var line = ""
                  var defaultVal = ""
                  do {
                    val possibleValues = new StringBuilder
                    if (interactiveConfiguration.choices().length != 0) {
                      possibleValues.append(" [")
                      for (i <- 0 until interactiveConfiguration.choices().length) {
                        possibleValues.append(interactiveConfiguration.choices()(i))
                        if (i < interactiveConfiguration.choices().length - 1) possibleValues += ','
                      }
                      possibleValues.append(']')
                    }

                    val label = new StringBuilder(interactiveConfiguration.label)
                    line = if (location.cyphered) {
                      label.append(": ")
                      new jline.ConsoleReader().readLine(label.toString, '*');
                    } else {
                      val oldVal = Activator.getWorkspace.preference(location)
                      defaultVal = if (oldVal == null || oldVal.isEmpty) Activator.getWorkspace.defaultValue(location) else oldVal
                      label.append(" (default=" + defaultVal + "; old=" + oldVal + ")" + possibleValues + ": ")
                      new jline.ConsoleReader().readLine(label.toString);
                    }
                  } while (interactiveConfiguration.choices().length != 0 && !interactiveConfiguration.choices().contains(line))

                  if (!line.isEmpty()) Activator.getWorkspace().setPreference(location, line)
                  else  Activator.getWorkspace().setPreference(location, defaultVal)

                }
              }
         //   }
          }
        }
      } catch {
        case e: ClassNotFoundException => Logger.getLogger(classOf[EnvironmentInitializer].getName).log(Level.FINE, c.getName() + "$ not found.");
      }
    }
  }

}
