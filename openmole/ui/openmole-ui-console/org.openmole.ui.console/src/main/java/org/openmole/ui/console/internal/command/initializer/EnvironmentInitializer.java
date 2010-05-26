/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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
import java.lang.reflect.Field;
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.core.workflow.model.execution.IEnvironment;
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

    @Override
    public void initialize(IEnvironment environment, Class object) {
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        for (Class c : SuperClassesLister.listSuperClasses(object)) {

            for (Field f : c.getDeclaredFields()) {
                InteractiveConfiguration interactiveConfiguration = f.getAnnotation(InteractiveConfiguration.class);
                if (interactiveConfiguration != null) {
                    if (ConfigurationLocation.class.isAssignableFrom(f.getType())) {
                        try {
                            boolean accessible = f.isAccessible();
                            f.setAccessible(true);
                            ConfigurationLocation location = (ConfigurationLocation) f.get(null);
                            f.setAccessible(accessible);
                            String line;
                            String label = interactiveConfiguration.label() + " (" + Activator.getWorkspace().getDefaultValue(location) + "): ";
                            if (location.isCyphered()) {
                                line = new jline.ConsoleReader().readLine(label, '*');
                            } else {
                                line = new jline.ConsoleReader().readLine(label);
                            }
                            if(!line.isEmpty()) {
                                Activator.getWorkspace().setPreference(location, line);
                            } else {
                                Activator.getWorkspace().removePreference(location);
                            }
                        } catch (IOException e) {
                            throw new Error(e);
                        } catch (InternalProcessingError e) {
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
