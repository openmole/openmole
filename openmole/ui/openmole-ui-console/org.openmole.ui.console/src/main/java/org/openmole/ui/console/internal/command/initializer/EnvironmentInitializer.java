/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.console.internal.command.initializer;

import java.io.IOException;
import java.lang.Class;
import java.lang.reflect.Field;
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.object.SuperClassesLister;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.workspace.InteractiveConfiguration;
import org.openmole.ui.console.internal.Activator;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class EnvironmentInitializer implements IInitializer<IEnvironment> {

    final Shell shell;

    public EnvironmentInitializer(Shell shell) {
        this.shell = shell;
    }
    
    static private boolean isValueInArray(String value, String[] values) {
        boolean ret = false;
        for (String v : values) {
            if (value.equals(v)) {
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public void initialize(IEnvironment environment, Class object) {
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (Class c : SuperClassesLister.listSuperClasses(object)) {

            //Look for companion object
            Class toSearch = c;
            Object obj = null;
            try {
                toSearch = org.codehaus.groovy.tools.shell.Groovysh.class.getClassLoader().loadClass(c.getName() + "$");
                obj = shell.execute(c.getName()+"$.MODULE$");
            } catch(ClassNotFoundException e) {
                System.out.println(c.getName() + "$ not found.");
            }
                    
            
            for (Field f : toSearch.getDeclaredFields()) {
                InteractiveConfiguration interactiveConfiguration = f.getAnnotation(InteractiveConfiguration.class);
                if (interactiveConfiguration != null) {
                    if (ConfigurationLocation.class.isAssignableFrom(f.getType())) {
                        try {
                            boolean accessible = f.isAccessible();
                            f.setAccessible(true);
                            ConfigurationLocation location = (ConfigurationLocation) f.get(obj);
                            f.setAccessible(accessible);

                            boolean enabled;

                            if (!interactiveConfiguration.dependOn().isEmpty()) {
                                String value = Activator.getWorkspace().preference(new ConfigurationLocation(location.group(), interactiveConfiguration.dependOn()));
                                enabled = value.equals(interactiveConfiguration.value());
                            } else {
                                enabled = true;
                            }

                            if (enabled) {

                                String line;
                                do {
                                    String possibleValues;
                                    if(interactiveConfiguration.choices().length != 0) {
                                        possibleValues = " [";
                                        for(int i = 0; i < interactiveConfiguration.choices().length; i++) {
                                            possibleValues += interactiveConfiguration.choices()[i];
                                            if(i < interactiveConfiguration.choices().length - 1) possibleValues += ','; 
                                        }
                                        possibleValues += ']';
                                    } else possibleValues = "";

                                    String label = interactiveConfiguration.label();
                                    if (location.cyphered()) {
                                        label += ": ";
                                        line = new jline.ConsoleReader().readLine(label, '*');
                                    } else {
                                        String oldVal = Activator.getWorkspace().preference(location);
                                        String defaultVal = Activator.getWorkspace().defaultValue(location);
                                       
                                        label += " (default=" + defaultVal + "; old=" + oldVal + ")" + possibleValues + ": ";
                                        line = new jline.ConsoleReader().readLine(label);
                                    }
                                } while (!line.isEmpty() && interactiveConfiguration.choices().length != 0 && !isValueInArray(line, interactiveConfiguration.choices()));

                                if (!line.isEmpty()) {
                                    Activator.getWorkspace().setPreference(location, line);
                                } else {
                                    Activator.getWorkspace().removePreference(location);
                                }
                            }
                        } catch (IOException e) {
                            throw new Error(e);
                        } catch (IllegalAccessException e) {
                            throw new Error(e);
                        }
                    }

                }
            }
        }
    }
}
