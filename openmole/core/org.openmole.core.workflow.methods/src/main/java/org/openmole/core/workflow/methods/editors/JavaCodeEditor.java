/*
 * JavaCodeEditor.java
 *
 *  Copyright (c) 2007, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
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

package org.openmole.core.workflow.methods.editors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.openmole.misc.exception.InternalProcessingError;

import sun.tools.evalexpr.MemoryFileManager;

/**
 * This class allow to compile on-the-fly a Java source code nested in a method,
 * as a script without need to declare a class or a method. Data objects can be
 * passed to the script with the argument of the method built.
 * 
 * There is methods for setting the imports statement needed by the code.
 */
public class JavaCodeEditor {
    
    private static final String CLASS_NAME = "JavaCodeClass";
    private static final String METHOD_NAME = "javaCodeMethod";
    
    private List<String> imports;
    private String code;
    private transient Method compiledMethod;
    
    public JavaCodeEditor() {
        compiledMethod = null;
        imports = new ArrayList<String>();
    }
    
    public boolean remove(String arg0) {
        compiledMethod = null;
        return imports.remove(arg0);
    }
    
    public boolean add(String arg0) {
        compiledMethod = null;
        return imports.add(arg0);
    }
    
    /**
     * These method builds the compiled Method ready to be called with your arguments.
     * 
     * @param args specify the type of arguments that will be passed to the method.
     * @return The method ready to be called.
     * @throws fr.cemagref.simexplorer.ide.core.InternalProcessingError 
     */
    public final Method compileMethod(Arg ... args) throws InternalProcessingError {
        if (compiledMethod == null) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            List<String> compilerFlags = new ArrayList();
            compilerFlags.add("-Xlint:all"); // report all warnings
            compilerFlags.add("-g:none"); // don't generate debug info
            // Use a customized file manager
            MemoryFileManager mfm =
                    new MemoryFileManager(compiler.getStandardFileManager(null, null, null));
            
            StringBuffer fileSourceCode = new StringBuffer(200);
            for (String importElt : imports) {
                fileSourceCode.append("import "+importElt+";\n");
            }
            StringBuffer methodArgs = new StringBuffer(30);
            Class[] argsTypes = new Class[args.length];
            int i = 0;
            for (Arg arg : args) {
                if (i > 0) {
                    methodArgs.append(", ");
                }
                argsTypes[i++] = arg.getType();
                methodArgs.append(arg.getType().getSimpleName());
                methodArgs.append(" ");
                methodArgs.append(arg.getName());
            }
            fileSourceCode.append("\npublic class " + CLASS_NAME + " {\n" );
            fileSourceCode.append("    public static void " + METHOD_NAME + "(" + methodArgs + ") throws Throwable {\n" );
            fileSourceCode.append("        " + this.code + "\n" );
            fileSourceCode.append("    }\n}\n");
            
            // Create a file object from a string
            JavaFileObject fileObject = MemoryFileManager.makeSource(CLASS_NAME,fileSourceCode.toString());
            
            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, mfm, null, compilerFlags, null, Arrays.asList(fileObject));
            if (task.call()) {
                try {
                    // Obtain a class loader for the compiled classes
                    ClassLoader cl = mfm.getClassLoader(null);
                    // Load the compiled class
                    Class compiledClass = cl.loadClass(CLASS_NAME);
                    // Find the eval method
                    this.compiledMethod = compiledClass.getMethod(METHOD_NAME, argsTypes);
                } catch (NoSuchMethodException ex) {
                    throw new InternalProcessingError(ex);
                } catch (SecurityException ex) {
                    throw new InternalProcessingError(ex);
                } catch (ClassNotFoundException ex) {
                    throw new InternalProcessingError(ex);
                }
            }
        }
        return this.compiledMethod;
    }
    
    /**
     * Call a method with its arguments.
     * 
     * @param method 
     * @param args 
     * @throws fr.cemagref.simexplorer.ide.core.InternalProcessingError if the method invokation throws an exception.
     */
    public void callMethod(Method method, Object... args) throws InternalProcessingError {
        try {
            // the method is static, so the instance is null
            method.invoke(null, args);
        } catch (IllegalAccessException ex) {
            throw new InternalProcessingError(ex);
        } catch (IllegalArgumentException ex) {
            throw new InternalProcessingError(ex);
        } catch (InvocationTargetException ex) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex.getTargetException());
            throw new InternalProcessingError(ex);
        }
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        compiledMethod = null;
        this.code = code;
    }
    
    public List<String> getImports() {
        return imports;
    }
    
    public void setImports(List<String> imports) {
        compiledMethod = null;
        this.imports = imports;
    }
    
    public static class Arg {
        private Class type;
        private String name;
        public Arg(Class type, String name) {
            this.type = type;
            this.name = name;
        }
        public Class getType() {
            return type;
        }
        public String getName() {
            return name;
        }
    }
}
