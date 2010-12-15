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
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.aspectj.bridge.IMessage;
import org.aspectj.tools.ajc.Main;
import org.codehaus.plexus.util.FileUtils;

/**
 * Base class for the two aspectJ compiletime weaving mojos.
 * 
 * @author <a href="mailto:kaare.nilsen@gmail.com">Kaare Nilsen</a>
 */
public abstract class AbstractAjcCompiler
        extends AbstractAjcMojo
{

    /**
     * The source directory for the aspects
     * @parameter default-value="src/main/aspect"
     */
    protected String aspectDirectory = "src/main/aspect";
    /**
     * The source directory for the test aspects
     * @parameter default-value="src/test/aspect"
     */
    protected String testAspectDirectory = "src/test/aspect";
    /**
     * List of ant-style patterns used to specify the aspects that should be included when 
     * compiling. When none specified all .java and .aj files in the project source directories, or
     * directories spesified by the ajdtDefFile property are included.
     * 
     * @parameter
     */
    protected String[] includes;
    /**
     * List of ant-style patterns used to specify the aspects that should be excluded when 
     * compiling. When none specified all .java and .aj files in the project source directories, or
     * directories spesified by the ajdtDefFile property are included.
     * 
     * @parameter
     */
    protected String[] excludes;
    /**
     * Where to find the ajdt build definition file.
     * <i>If set this will override the use of project sourcedirs</i>.
     * 
     * @parameter
     */
    protected String ajdtBuildDefFile;
    /**
     * Generate aop.xml file for load-time weaving with default name.(/META-INF/aop.xml)
     * 
     * @parameter
     */
    protected boolean outxml;
    /**
     * Generate aop.xml file for load-time weaving with custom name.
     * 
     * @parameter
     */
    protected String outxmlfile;
    /**
     * Generate .ajesym symbol files for emacs support
     * 
     *  @parameter
     */
    protected boolean emacssym;
    /**
     * Set default level for messages about potential 
     * programming mistakes in crosscutting code. 
     * {level} may be ignore, warning, or error. 
     * This overrides entries in org/aspectj/weaver/XlintDefault.properties 
     * from aspectjtools.jar.
     * 
     *  @parameter
     */
    protected String Xlint;
    /**
     * Enables the compiler to support hasmethod(method_pattern) 
     * and hasfield(field_pattern) type patterns,
     * but only within declare statements. 
     * 
     * It's experimental and undocumented because it may change, 
     * and because it doesn't yet take into account ITDs. 
     *  
     * @parameter
     * @since 1.3
     */
    protected boolean XhasMember;
    /**
     * Specify classfile target setting (1.1 to 1.6) default is 1.2
     * 
     *  @parameter default-value="${project.build.java.target}"
     */
    protected String target;
    /**
     * Toggle assertions (1.3, 1.4, or 1.6 - default is 1.4). 
     * When using -source 1.3, an assert() statement valid under Java 1.4 
     * will result in a compiler error. When using -source 1.4, 
     * treat assert as a keyword and implement assertions 
     * according to the 1.4 language spec. 
     * When using -source 1.5 or higher, Java 5 language features are permitted.
     * 
     *  @parameter default-value="${project.build.java.target}"
     */
    protected String source;
    /**
     * Specify compiler compliance setting (1.3 to 1.6)
     * default is 1.4
     * 
     *  @parameter
     */
    protected String complianceLevel;
    /**
     * Toggle warningmessages on deprecations
     * 
     * @parameter
     */
    protected boolean deprecation;
    /**
     * Emit no errors for unresolved imports;
     * 
     * @parameter
     */
    protected boolean noImportError;
    /**
     * Keep compiling after error, dumping class files with problem methods
     * 
     * @parameter
     */
    protected boolean proceedOnError;
    /**
     * Preserve all local variables during code generation (to facilitate debugging).
     * 
     * @parameter
     */
    protected boolean preserveAllLocals;
    /**
     * Compute reference information.
     * 
     * @parameter
     */
    protected boolean referenceInfo;
    /**
     * Specify default source encoding format. 
     *      
     * @parameter expression="${project.build.sourceEncoding}"
     */
    protected String encoding;
    /**
     * Emit messages about accessed/processed compilation units
     * 
     * @parameter
     */
    protected boolean verbose;
    /**
     * Emit messages about weaving
     * 
     * @parameter
     */
    protected boolean showWeaveInfo;
    /**
     * Repeat compilation process N times (typically to do performance analysis).
     * 
     * @parameter
     */
    protected int repeat;
    /**
     * (Experimental) runs weaver in reweavable mode which causes it to create 
     * woven classes that can be rewoven, subject to the restriction 
     * that on attempting a reweave all the types that advised the woven 
     * type must be accessible.
     * 
     * @parameter
     */
    protected boolean Xreweavable;
    /**
     * (Experimental) do not inline around advice
     * 
     * @parameter
     */
    protected boolean XnoInline;
    /**
     * (Experimental) Normally it is an error to declare aspects Serializable. This option removes that restriction.
     * 
     * @parameter
     */
    protected boolean XserializableAspects;
    /**
     * Causes the compiler to calculate and add the SerialVersionUID field to any type implementing Serializable that is affected by an aspect. 
     * The field is calculated based on the class before weaving has taken place.
     * 
     * @parameter
     */
    protected boolean XaddSerialVersionUID;
    /**
     * Override location of VM's bootclasspath for purposes of evaluating types when compiling. 
     * Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
     *
     * @parameter
     */
    protected String bootclasspath;
    /**
     * Emit warnings for any instances of the comma-delimited list of questionable code (eg 'unusedLocals,deprecation'):
     * see http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html#ajc for available settings
     * @parameter
     */
    protected String warn;
    /**
     * The filename to store build configuration in.
     * This file will be placed in the project build output
     * directory, and will contain all the arguments
     * passed to the compiler in the last run, and also
     * all the filenames included in the build. Aspects as
     * well as java files.
     * 
     * @parameter default-value="builddef.lst"
     */
    protected String argumentFileName = "builddef.lst";
    /**
     * Forces re-compilation, regardless of whether the compiler arguments or
     * the sources have changed.
     * 
     * @parameter
     */
    protected boolean forceAjcCompile;
    /**
     * Holder for ajc compiler options
     */
    protected List ajcOptions = new LinkedList();

    /**
     * Holds all files found using the includes, excludes parameters.
     */
    /**
     * Abstract method used by child classes to spesify the correct output
     * directory for compiled classes.
     * 
     * @return where compiled classes should be put.
     */
    protected abstract List<String> getOutputDirectories();

    /**
     * Abstract method used by child classes to spesify the correct source directory for classes.
     * 
     * @return where sources may be found.
     */
    protected abstract List<String> getSourceDirectories();

    /**
     * Abstract method used by cild classes to specify aditional aspect paths.
     * @return
     */
    protected abstract String getAdditionalAspectPaths();

    /**
     * Do the AspectJ compiling.
     * 
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().warn( "Not executing aspectJ compiler as the project is not a Java classpath-capable package" );
            return;
        }

        Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
        project.getCompileSourceRoots().add( basedir.getAbsolutePath() + "/" + aspectDirectory );
        project.getTestCompileSourceRoots().add( basedir.getAbsolutePath() + "/" + testAspectDirectory );

        Set<String> resolvedIncludes = new TreeSet<String>();
        List<String> localAjcOptions = new LinkedList<String>();
        localAjcOptions.addAll( ajcOptions );
        assembleArguments( localAjcOptions, resolvedIncludes );


        /*----------------- AspectJ source compiling ------------------------*/

        if ( hasSourcesToCompile( resolvedIncludes ) )
        {
            if ( forceAjcCompile || isBuildNeeded( resolvedIncludes ) )
            {

                getLog().info( "Compiling aspects." );

                if ( getLog().isDebugEnabled() )
                {
                    String command = "Running : ajc ";
                    Iterator iter = localAjcOptions.iterator();
                    while ( iter.hasNext() )
                    {
                        command += ( iter.next() + " " );
                    }
                    getLog().debug( command );
                }
                try
                {
                    getLog().debug( "Compiling and weaving " + resolvedIncludes.size() + " sources to " + getOutputDirectories().get( 0 ) );
                    File outDir = new File( ( String ) getOutputDirectories().get( 0 ) );
                    AjcHelper.writeBuildConfigToFile( localAjcOptions, argumentFileName, outDir );
                    getLog().debug(
                            "Argumentsfile written : "
                            + new File( outDir.getAbsolutePath() + argumentFileName ).getAbsolutePath() );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not write arguments file to the target area", e );
                }

                Main main = new Main();
                MavenMessageHandler mavenMessageHandler = new MavenMessageHandler( getLog() );
                main.setHolder( mavenMessageHandler );

                main.runMain( ( String[] ) localAjcOptions.toArray( new String[0] ), false );
                IMessage[] errors = mavenMessageHandler.getMessages( IMessage.ERROR, true );
                if ( !proceedOnError && errors.length > 0 )
                {
                    throw new CompilationFailedException( errors );
                }
            }
        }

        /*----------------- AspectJ weaving ------------------------*/

        File outputDir = new File( ( String ) getOutputDirectories().get( 0 ) );

        if ( outputDir.exists() && outputDir.isDirectory() && outputDir.list().length > 0 )
        {

            localAjcOptions = new LinkedList<String>();
            localAjcOptions.addAll( ajcOptions );

            getLog().info( "Weaving aspects." );

            Main main = new Main();
            MavenMessageHandler mavenMessageHandler = new MavenMessageHandler( getLog() );
            main.setHolder( mavenMessageHandler );

            //addModulesArgument( "-inpath", localAjcOptions, weaveDependencies, null, "a dependency to weave" );
            localAjcOptions.add( "-d" );
            localAjcOptions.add( getOutputDirectories().get( 0 ).toString() );
            localAjcOptions.add( "-classpath" );
            localAjcOptions.add( AjcHelper.createClassPath( project, null, getOutputDirectories() ) );
            localAjcOptions.add( "-inpath" );
            localAjcOptions.add( getOutputDirectories().get( 0 ).toString() );
            // localAjcOptions.add( "-aspectpath" );
            addModulesArgument( "-aspectpath", localAjcOptions, weaveDependencies, null, "a dependency to weave" );
            // localAjcOptions.add( AjcHelper.createClassPath( project, null, getOutputDirectories() ) );

            //getLog().info( "-d " + getOutputDirectories().get( 0 ).toString() + " -showWeaveInfo " + " -classpath " + AjcHelper.createClassPath( project, null, getOutputDirectories() ) + " -inpath " + getOutputDirectories().get( 0 ).toString() + " -aspectpath "+ AjcHelper.createClassPath( project, null, getOutputDirectories() ));
            if ( getLog().isDebugEnabled() )
            {
                String command = "Running : ajc ";
                Iterator iter = localAjcOptions.iterator();
                while ( iter.hasNext() )
                {
                    command += ( iter.next() + " " );
                }
                getLog().debug( command );
            }
            // getLog().info(AjcHelper.createClassPath( project, null, getOutputDirectories()) + " -inpath " + getOutputDirectories().get( 0 ).toString());
            main.runMain( localAjcOptions.toArray( new String[0] ), false );

            IMessage[] errors = mavenMessageHandler.getMessages( IMessage.ERROR, true );
            if ( !proceedOnError && errors.length > 0 )
            {
                throw new CompilationFailedException( errors );
            }
        }
    }

    /**
     * Assembles a complete ajc compiler arguments list.
     *
     * @throws MojoExecutionException error in configuration
     */
    protected void assembleArguments( List<String> ajcOptions, Set<String> resolvedIncludes ) throws MojoExecutionException
    {
        if ( XhasMember )
        {
            ajcOptions.add( "-XhasMember" );
        }

        // Add classpath
        ajcOptions.add( "-classpath" );
        ajcOptions.add( AjcHelper.createClassPath( project, null, getOutputDirectories() ) );

        // Add boot classpath
        if ( null != bootclasspath )
        {
            ajcOptions.add( "-bootclasspath" );
            ajcOptions.add( bootclasspath );
        }

        // Add warn option
        if ( null != warn )
        {
            ajcOptions.add( "-warn:" + warn );
        }

        // Add artifacts to weave
        addModulesArgument( "-inpath", ajcOptions, weaveDependencies, null, "a dependency to weave" );

        // Add library artifacts 
        addModulesArgument( "-aspectpath", ajcOptions, aspectLibraries, getAdditionalAspectPaths(), "an aspect library" );

        //add target dir argument
        ajcOptions.add( "-d" );
        ajcOptions.add( getOutputDirectories().get( 0 ) );

        // Add all the files to be included in the build,
        if ( null != ajdtBuildDefFile )
        {
            resolvedIncludes.addAll( AjcHelper.getBuildFilesForAjdtFile( ajdtBuildDefFile, basedir ) );
        }
        else
        {
            resolvedIncludes.addAll( AjcHelper.getBuildFilesForSourceDirs( getSourceDirectories(), this.includes,
                    this.excludes ) );
        }
        ajcOptions.addAll( resolvedIncludes );
    }

    /**
     * Finds all artifacts in the weavemodule property,
     * and adds them to the ajc options.
     *  
     * @param arguments
     * @throws MojoExecutionException
     */
    private void addModulesArgument( String argument, List arguments, Module[] modules, String aditionalpath,
            String role )
            throws MojoExecutionException
    {
        StringBuffer buf = new StringBuffer();

        if ( null != aditionalpath )
        {
            arguments.add( argument );
            buf.append( aditionalpath );
        }
        if ( modules != null && modules.length > 0 )
        {
            if ( !arguments.contains( argument ) )
            {
                arguments.add( argument );
            }

            for ( int i = 0; i < modules.length; ++i )
            {
                Module module = modules[i];
                String key = ArtifactUtils.versionlessKey( module.getGroupId(), module.getArtifactId() );
                Artifact artifact = ( Artifact ) project.getArtifactMap().get( key );
                if ( artifact == null )
                {
                    throw new MojoExecutionException( "The artifact " + key + " referenced in aspectj plugin as "
                            + role + ", is not found the project dependencies" );
                }
                if ( buf.length() != 0 )
                {
                    buf.append( File.pathSeparatorChar );
                }
                buf.append( artifact.getFile().getPath() );
            }
        }
        if ( buf.length() > 0 )
        {
            String pathString = buf.toString();
            arguments.add( pathString );
            getLog().debug( "Adding " + argument + ": " + pathString );
        }
    }

    /**
     * Checks modifications that would make us need a build
     * 
     * @throws MojoExecutionException 
     *
     */
    protected boolean isBuildNeeded( Set<String> resolvedIncludes )
            throws MojoExecutionException
    {
        File outDir = new File( getOutputDirectories().get( 0 ).toString() );
        return hasNoPreviousBuild( outDir ) || hasArgumentsChanged( outDir ) || hasSourcesChanged( outDir, resolvedIncludes );

    }

    private boolean hasNoPreviousBuild( File outDir )
    {
        return ( !FileUtils.fileExists( new File( outDir.getAbsolutePath(), argumentFileName ).getAbsolutePath() ) );
    }

    private boolean hasArgumentsChanged( File outDir )
            throws MojoExecutionException
    {
        try
        {
            return ( !ajcOptions.equals( AjcHelper.readBuildConfigFile( argumentFileName, outDir ) ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during reading of previous argumentsfile " );
        }
    }

    /**
     * Not entirely safe, assembleArguments() must be run 
     */
    private boolean hasSourcesToCompile( Set<String> resolvedIncludes )
    {
        return resolvedIncludes.size() > 0;
    }

    private boolean hasSourcesChanged( File outDir, Set<String> resolvedIncludes )
    {
        Iterator sourceIter = resolvedIncludes.iterator();
        long lastBuild = new File( outDir.getAbsolutePath(), argumentFileName ).lastModified();
        while ( sourceIter.hasNext() )
        {
            File sourceFile = new File( ( String ) sourceIter.next() );
            long sourceModified = sourceFile.lastModified();
            if ( sourceModified >= lastBuild )
            {
                return true;
            }

        }
        return false;
    }

    /** 
     * Setters which when called sets compiler arguments
     */
    public void setComplianceLevel( String complianceLevel )
    {
        if ( complianceLevel.equals( "1.3" ) || complianceLevel.equals( "1.4" ) || complianceLevel.equals( "1.5" ) || complianceLevel.equals( "1.6" ) )
        {
            ajcOptions.add( "-" + complianceLevel );
        }

    }

    public void setDeprecation( boolean deprecation )
    {
        if ( deprecation )
        {
            ajcOptions.add( "-deprecation" );
        }
    }

    public void setEmacssym( boolean emacssym )
    {
        if ( emacssym )
        {
            ajcOptions.add( "-emacssym" );
        }

    }

    public void setEncoding( String encoding )
    {
        ajcOptions.add( "-encoding" );
        ajcOptions.add( encoding );
    }

    public void setNoImportError( boolean noImportError )
    {
        if ( noImportError )
        {
            ajcOptions.add( "-noImportError" );
        }

    }

    public void setOutxml( boolean outxml )
    {
        if ( outxml )
        {
            ajcOptions.add( "-outxml" );
        }

    }

    public void setOutxmlfile( String outxmlfile )
    {
        ajcOptions.add( "-outxmlfile" );
        ajcOptions.add( outxmlfile );
    }

    public void setPreserveAllLocals( boolean preserveAllLocals )
    {
        if ( preserveAllLocals )
        {
            ajcOptions.add( "-preserveAllLocals" );
        }

    }

    public void setProceedOnError( boolean proceedOnError )
    {
        if ( proceedOnError )
        {
            ajcOptions.add( "-proceedOnError" );
        }
        this.proceedOnError = proceedOnError;
    }

    public void setReferenceInfo( boolean referenceInfo )
    {
        if ( referenceInfo )
        {
            ajcOptions.add( "-referenceInfo" );
        }

    }

    public void setRepeat( int repeat )
    {
        ajcOptions.add( "-repeat" );
        ajcOptions.add( "" + repeat );
    }

    public void setShowWeaveInfo( boolean showWeaveInfo )
    {
        if ( showWeaveInfo )
        {
            ajcOptions.add( "-showWeaveInfo" );
        }

    }

    public void setTarget( String target )
    {
        ajcOptions.add( "-target" );
        ajcOptions.add( target );
    }

    public void setSource( String source )
    {
        ajcOptions.add( "-source" );
        ajcOptions.add( source );
    }

    public void setVerbose( boolean verbose )
    {
        if ( verbose )
        {
            ajcOptions.add( "-verbose" );
        }
    }

    public void setXhasMember( boolean xhasMember )
    {
        XhasMember = xhasMember;
    }

    public void setXlint( String xlint )
    {
        ajcOptions.add( "-Xlint:" + xlint );
    }

    public void setXnoInline( boolean xnoInline )
    {
        if ( xnoInline )
        {
            ajcOptions.add( "-XnoInline" );
        }

    }

    public void setXreweavable( boolean xreweavable )
    {
        if ( xreweavable )
        {
            ajcOptions.add( "-Xreweavable" );
        }

    }

    public void setXserializableAspects( boolean xserializableAspects )
    {
        if ( xserializableAspects )
        {
            ajcOptions.add( "-XserializableAspects" );
        }
    }

    public void setXaddSerialVersionUID( boolean xaddSerialVersionUID )
    {
        if ( xaddSerialVersionUID )
        {
            ajcOptions.add( "-XaddSerialVersionUID" );
        }
    }

    public void setBootClassPath( String bootclasspath )
    {
        this.bootclasspath = bootclasspath;
    }

    public void setWarn( String warn )
    {
        this.warn = warn;
    }

    public void setArgumentFileName( String argumentFileName )
    {
        this.argumentFileName = argumentFileName;

    }
}
