package org.codehaus.mojo.aspectj;

/**
 * The MIT License
 *
 * Copyright 2005-2006 The Codehaus.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * The base class.
 * 
 * @author Juraj Burian
 * @version $Revision: 11578 $ by $Author: david $
 */
abstract public class AbstractAjcMojo extends AbstractMojo
{
    /**
     * The maven project.
     * 
     * @parameter default-value="${project}"
     * @required 
     * @readonly
     */
    protected MavenProject project;

    /**
     * The basedir of the project.
     * 
     * @parameter expression="${basedir}"
     * @required 
     * @readonly
     */
    protected File basedir;
    
    /**
     * List of of modules to weave (into target directory). Corresponds to ajc
     * -inpath option (or -injars for pre-1.2 (which is not supported)).
     * 
     * @parameter
     */
    protected Module[] weaveDependencies;

    /**
     * Weave binary aspects from the jars. 
     * The aspects should have been output by the same version of the compiler. 
     * The modules must also be dependencies of the project.
     * Corresponds to ajc -aspectpath option
     * 
     * @parameter
     */
    protected Module[] aspectLibraries;


}
