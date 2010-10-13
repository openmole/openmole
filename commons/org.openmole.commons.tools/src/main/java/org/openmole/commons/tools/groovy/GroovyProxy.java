/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.commons.tools.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.File;
import java.net.MalformedURLException;

//import org.codehaus.groovy.ant.Groovy;
//import java.util.logging.Logger;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

public class GroovyProxy implements IGroovyProxy {

    private transient Script compiledScript;

    /**
     * Use this method to give the groovy code.
     * Your script is automatically compiled, ready to be executed later.
     * @param code
     */
    public void compile(String code, Iterable<File> jars) throws InternalProcessingError, UserBadDataError {
        GroovyShell groovyShell = new GroovyShell();
        for(File jar: jars) {
            try {
                groovyShell.getClassLoader().addURL(jar.toURI().toURL());
            } catch (MalformedURLException ex) {
                throw new InternalProcessingError(ex);
            }
        }
        try {
            compiledScript = groovyShell.parse("package script\n" + code);
        } catch(Throwable t) {
            throw new UserBadDataError("Script compilation error !\n The script was :\n" + code + "\n Error message was:" + t.getMessage());
        }
    }

    public boolean isScriptCompiled() {
        return compiledScript != null;
    }

    /**
     * This method run your compiled script.
     * @return the result of your script if a variable is returned.
     * @throws InternalProcessingError 
     */
    public Object execute(Binding binding) {
        compiledScript.setBinding(binding);
        Object ret = compiledScript.run();
        InvokerHelper.removeClass(compiledScript.getClass());
        compiledScript.setBinding(null);
        return ret;
    }
}
