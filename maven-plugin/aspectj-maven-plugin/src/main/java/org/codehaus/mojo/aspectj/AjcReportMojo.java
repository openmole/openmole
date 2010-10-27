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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.aspectj.tools.ajdoc.Main;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Creates a ajdoc report in html format.
 *
 * @description A Maven 2.0 ajdoc report
 * @author       <a href="mailto:kaare.nilsen@gmail.com">Kaare Nilsen</a>
 * @goal aspectj-report
 */
public class AjcReportMojo
    extends AbstractMavenReport
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
     * List of ant-style patterns used to specify the aspects that should be included when 
     * compiling. When none specified all .java and .aj files in the project source directories, or
     * directories spesified by the ajdtDefFile property are included.
     */
    protected String[] includes;

    /**
     * List of ant-style patterns used to specify the aspects that should be excluded when 
     * compiling. When none specified all .java and .aj files in the project source directories, or
     * directories spesified by the ajdtDefFile property are included.
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
     * Doxia Site Renderer.
     *
     * @parameter default-value="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * Shows only package, protected, and public classes and members.
     * 
     * @parameter
     */
    protected boolean packageScope;

    /**
     * Shows only protected and public classes and members. This is the default.
     * 
     * @parameter
     */
    protected boolean protectedScope;

    /**
     * Shows all classes and members.
     * 
     * @parameter
     */
    protected boolean privateScope;

    /**
     * Shows only public classes and members.
     * 
     * @parameter
     */
    protected boolean publicScope;

    /**
     * Specifies that javadoc should retrieve the text for the overview documentation from the "source" file specified by path/filename and place it on the Overview page (overview-summary.html). 
     * The path/filename is relative to the ${basedir}. While you can use any name you want for filename and place it anywhere you want for path,
     *  a typical thing to do is to name it overview.html and place it in the source tree at the directory that contains the topmost package directories. 
     *  In this location, no path is needed when documenting packages, since -sourcepath will point to this file. For example, if the source tree for the 
     *  java.lang package is /src/classes/java/lang/, then you could place the overview file at /src/classes/overview.html. See Real World Example.
     *  For information about the file specified by path/filename, see overview comment file.Note that the overview page is created only if you pass into javadoc two or more package names. 
     *  For further explanation, see HTML Frames.) The title on the overview page is set by -doctitle. 
     * 
     * @parameter
     */
    protected String overview;

    /**
     * Specifies the title to be placed near the top of the overview summary file. The title will be placed as a centered, 
     * level-one heading directly beneath the upper navigation bar. The title may contain html tags and white space, though if 
     * it does, it must be enclosed in quotes. Any internal quotation marks within title may have to be escaped.
     * @parameter
     */
    protected String doctitle;

    /**
     * Provides more detailed messages while javadoc is running. Without the verbose option, messages appear for loading the source files, generating the documentation (one message per source file), 
     * and sorting. The verbose option causes the printing of additional messages specifying the number of milliseconds to parse each java source file.
     * 
     * @parameter
     */
    protected boolean verbose;
    
    /**
     * Specify compiler compliance setting (1.3 to 1.6, default is 1.4)
     * 
     *  @parameter default-value="${project.build.java.target}"
     */
    protected String complianceLevel;

    /**
     * Holder for all options passed
     */
    private List ajcOptions = new ArrayList();

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginArtifacts;
    
    /**
     * Executes this ajdoc-report generation.
     * 
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        getLog().info( "Starting generating ajdoc" );
        Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
        project.getCompileSourceRoots().add( basedir.getAbsolutePath() + "/" + aspectDirectory );
        project.getTestCompileSourceRoots().add( basedir.getAbsolutePath() + "/" + testAspectDirectory );
        
        ArrayList arguments = new ArrayList();
        // Add classpath
        arguments.add( "-classpath" );
        arguments.add( AjcHelper.createClassPath( project, pluginArtifacts, getOutputDirectories() ) );

        arguments.addAll( ajcOptions );

        Set includes;
        try
        {
            if ( null != ajdtBuildDefFile )
            {
                includes = AjcHelper.getBuildFilesForAjdtFile( ajdtBuildDefFile, basedir );
            }
            else
            {
                includes = AjcHelper.getBuildFilesForSourceDirs( getSourceDirectories(), this.includes, this.excludes );
            }
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "AspectJ Report failed", e );
        }

        // add target dir argument
        arguments.add( "-d" );
        arguments.add( StringUtils.replace( getOutputDirectory(), "//", "/" ) );

        arguments.addAll( includes );

        if ( getLog().isDebugEnabled() )
        {
            String command = "Running : ajdoc ";
            Iterator iter = arguments.iterator();
            while ( iter.hasNext() )
            {
                command += ( iter.next() + " " );
            }
            getLog().debug( command );
        }

        Main.main( (String[]) arguments.toArray( new String[0] ) );

    }

    /**
     * Get the directories containg sources 
     */
    protected List getSourceDirectories()
    {
        List sourceDirectories = new ArrayList();
        sourceDirectories.addAll( project.getCompileSourceRoots() );
        sourceDirectories.addAll( project.getTestCompileSourceRoots() );
        return sourceDirectories;
    }

    /**
     * get report output directory.
     */
    protected String getOutputDirectory()
    {
        return project.getBuild().getDirectory() + "/site/aspectj-report";
    }

    /**
     * get compileroutput directory.
     */
    protected List getOutputDirectories()
    {
        return Arrays.asList( new String[] {
            project.getBuild().getOutputDirectory(),
            project.getBuild().getTestOutputDirectory() } );
    }

    /**
     * 
     */
    public String getOutputName()
    {
        return "aspectj-report/index";
    }

    /**
     * 
     */
    public String getName( Locale locale )
    {
        return "aspectJ";
    }

    /**
     * 
     */
    public String getDescription( Locale locale )
    {
        return " Similar to javadoc, Maven AspectJ Report renders HTML" + " documentation for "
            + "pointcuts, advice, and inter-type declarations, as well as the"
            + " Java constructs that Javadoc renders. Maven AspectJ Report also" + " links advice"
            + " from members affected by the advice and the inter-type "
            + "declaration for members declared from aspects. The aspect will"
            + " be fully documented, as will your target classes, including "
            + "links to any advice or declarations that affect the class. "
            + "That means, for example, that you can see everything affecting"
            + " a method when reading the documentation for the method.";
    }
    
    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#isExternalReport()
     */
    public boolean isExternalReport()
    {
        return true;
    }
    
    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // Only execute reports for java projects
        ArtifactHandler artifactHandler = this.project.getArtifact().getArtifactHandler();
        return "java".equals( artifactHandler.getLanguage() );
    }
    

    /**
     * Get the site renderer.
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }


    /**
     * Get the maven project.
     */
    protected MavenProject getProject()
    {
        return project;
    }

    public void setOverview( String overview )
    {
        ajcOptions.add( "-overview" );
        ajcOptions.add( overview );
    }

    public void setDoctitle( String doctitle )
    {
        ajcOptions.add( "-doctitle" );
        ajcOptions.add( doctitle );
    }

    public void setPackageScope( boolean packageScope )
    {
        if ( packageScope )
        {
            ajcOptions.add( "-package" );
        }
    }

    public void setPrivateScope( boolean privateScope )
    {
        if ( privateScope )
        {
            ajcOptions.add( "-private" );
        }
    }

    public void setProtectedScope( boolean protectedScope )
    {
        if ( protectedScope )
        {
            ajcOptions.add( "-protected" );
        }
    }

    public void setPublicScope( boolean publicScope )
    {
        if ( publicScope )
        {
            ajcOptions.add( "-public" );
        }
    }

    public void setVerbose( boolean verbose )
    {
        if ( verbose )
        {
            ajcOptions.add( "-verbose" );
        }
    }
    /** 
     * Setters which when called sets compiler arguments
     */
    public void setComplianceLevel( String complianceLevel )
    {
        if ( complianceLevel.equals( "1.3" ) || complianceLevel.equals( "1.4" ) || complianceLevel.equals( "1.5" ) || complianceLevel.equals( "1.6" ) )
        {
            ajcOptions.add( "-source" );
            ajcOptions.add( complianceLevel );
        }

    }

    public void setPluginArtifacts( List pluginArtifacts )
    {
        this.pluginArtifacts = pluginArtifacts;

    }    
}
