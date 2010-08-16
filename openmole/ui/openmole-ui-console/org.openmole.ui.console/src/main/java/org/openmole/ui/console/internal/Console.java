/*
 *  Copyright (C) 2010 Cemagref
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.ui.console.internal;

import groovy.lang.Binding;
import org.codehaus.groovy.ant.Groovy;
import org.codehaus.groovy.tools.shell.Command;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.openmole.ui.console.IConsole;

/**
 *
 * @author dumoulin
 */
public class Console implements IConsole {

    private Groovysh groovysh;
    private Binding binding;

    public Binding getBinding() {
        return binding;
    }

    public Groovysh getGroovysh() {
        return groovysh;
    }

    public Console() {
        this.binding = new Binding();
        this.groovysh = new Groovysh(Groovy.class.getClassLoader(), binding, new IO());
    }

    @Override
    public void setVariable(String name, Object value) {
        binding.setVariable(name, value);
    }

    @Override
    public void run(String command) {
        groovysh.run(command);
    }

    @Override
    public Object leftShift(Command cmnd) {
        return groovysh.leftShift(cmnd);
    }

}
