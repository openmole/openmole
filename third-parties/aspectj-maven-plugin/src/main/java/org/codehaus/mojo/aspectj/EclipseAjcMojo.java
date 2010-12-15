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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Create eclipse configuration of aspectJ
 * 
 * @author Juraj Burian
 * @version $Revision: 7481 $ by $Author: david $
 * 
 * (at)goal eclipse
 * @requiresDependencyResolution compile
 * @description create eclipse configuration of aspectJ
 */
public class EclipseAjcMojo extends AbstractAjcMojo
{
    public static final String FILE_SEPARATOR = //
    System.getProperty( "file.separator" );

    private final String[] ASPECT_LIBRARIES_KEYS = //
    new String[] { "org.eclipse.ajdt.ui.aspectPath.contentKind",
            "org.eclipse.ajdt.ui.aspectPath.entryKind",
            "org.eclipse.ajdt.ui.aspectPath" };

    private final String[] WEAVE_DEPENDENCIES_KEYS = //
    new String[] { "org.eclipse.ajdt.ui.inPath.contentKind",
            "org.eclipse.ajdt.ui.inPath.entryKind",
            "org.eclipse.ajdt.ui.inPath" };

    private static final String AJ_BUILDER = "org.eclipse.ajdt.core.ajbuilder";

    private static final String M2_BUILDER = "org.maven.ide.eclipse.maven2Builder";

    private static final String AJ_NATURE = "org.eclipse.ajdt.ui.ajnature";

    public void execute() throws MojoExecutionException
    {
        // exclude this :
        if( "pom".endsWith( project.getPackaging() )
                || "ear".endsWith( project.getPackaging() ) )
        {
            return;
        }

        // write file
        File prefs = new File( //
                basedir, ".settings" + FILE_SEPARATOR
                        + "org.eclipse.ajdt.ui.prefs" );
        try
        {
            prefs.getParentFile().mkdirs();
            prefs.createNewFile();
        } catch ( IOException e )
        {
            throw new MojoExecutionException( //
                    "Can't create file: " + prefs.getPath() );
        }

        PrintWriter out = null;
        try
        {
            out = new PrintWriter( new FileOutputStream( prefs ) );
        } catch ( FileNotFoundException e )
        {
            // can't happen
        }
        out.println( "#" + new Date() );
        out.println( "eclipse.preferences.version=1" );
        writePaths( out, aspectLibraries, ASPECT_LIBRARIES_KEYS );
        writePaths( out, weaveDependencies, WEAVE_DEPENDENCIES_KEYS );
        out.close();

        // merge .project file if exists
        File dotProject = new File( basedir, ".project" );
        if( dotProject.exists() )
        {
            mergeProject(dotProject);
        }
    }

    protected List getOutputDirectories()
    {
        return null;
    }

    protected List getSourceDirectories()
    {
        return null;
    }

    private final void writePaths( //
            PrintWriter out, Module[] modules, String[] keys )
            throws MojoExecutionException
    {
        if( modules == null || modules.length == 0 )
        {
            return;
        }
        String[] paths = new String[modules.length];
        for( int i = 0; i < modules.length; i++ )
        {
            Module module = modules[i];
            String key = ArtifactUtils.versionlessKey( module.getGroupId(),
                    module.getArtifactId() );
            Artifact artifact = (Artifact) project.getArtifactMap().get( key );
            if( artifact == null )
            {
                throw new MojoExecutionException(
                        "The artifact "
                                + key
                                + " referenced in aspectj plugin as an aspect library, is not found the project dependencies" );

            }
            paths[i] = artifact.getFile().getPath();
        }
        for( int i = 1; i <= paths.length; i++ )
        {
            out.println( "org.eclipse.ajdt.ui.aspectPath.contentKind" + i
                    + "=BINARY" );
        }
        for( int i = 1; i <= paths.length; i++ )
        {
            out.println( "org.eclipse.ajdt.ui.aspectPath.entryKind" + i
                    + "=LIBRARY" );
        }
        for( int i = 0; i < paths.length; i++ )
        {
            out.print( "org.eclipse.ajdt.ui.aspectPath" + i + "=" );
            String path = paths[i];
            path = StringUtils.replace( path, "\\", "/" );
            path = StringUtils.replace( path, ":", "\\:" );
            out.println( path );
        }
    }

    /**
     * @throws  
     * 
     * 
     */
    private void mergeProject( File file ) throws MojoExecutionException
    {
        try
        {
            DocumentBuilder builder = //
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse( file );
            boolean builderMerged = mergeBuilders( document );
            boolean natureMerged = mergeNatures( document );
            if( builderMerged 
                    || natureMerged )
            {
                writeDocument( document, file );
            }

        } catch ( ParserConfigurationException ex )
        {
            throw new MojoExecutionException( "Can't create doom parser configuration", ex );
            
        } catch ( SAXException ex )
        {
            throw new MojoExecutionException( "Can't parse .project file", ex );
            
        } catch ( Exception ex )
        {
            throw new MojoExecutionException( "Can't merge .project file", ex );
        }    }

    /**
     * 
     * @param document
     * @return true if document need be saved
     */
    // TODO remove javac builder if aspectJ builder is used
    private boolean mergeBuilders( Document document )
            throws MojoExecutionException
    {
        NodeList buildCommandList = document
                .getElementsByTagName( "buildCommand" );
        for( int i = 0; i < buildCommandList.getLength(); i++ )
        {
            Element buildCommand = (Element) buildCommandList.item( i );
            NodeList nameList = buildCommand.getElementsByTagName( "name" );
            for( int j = 0; j < nameList.getLength(); j++ )
            {
                Element name = (Element) nameList.item( j );
                if( name.getNodeValue().equals( AJ_BUILDER ) )
                {
                    return false;
                }
                // if maven2 builder is used we don't need
                // use aspectJ builder
                if( name.getNodeValue().equals( M2_BUILDER ) )
                {
                    return false;
                }
            }
        }
        // we need add aspectJ builder node
        NodeList buildSpecList = document.getElementsByTagName( "buildSpec" );
        if( 0 == buildSpecList.getLength() )
        {
            NodeList nodes = document.getElementsByTagName( "natures" );
            if( 0 == nodes.getLength() )
            {
                throw new MojoExecutionException(
                        "At least one nature must be specified in .project file!" );
            }
            Element buildSpec = document.createElement( "buildSpec" );
            document.insertBefore( buildSpec, nodes.item( 0 ) );
            buildSpecList = document.getElementsByTagName( "buildSpec" );
        }
        Element buildSpec = (Element) buildSpecList.item( 0 );
        Element buildCommand = document.createElement( "buildCommand" );

        // create & append <name/>
        Element name = document.createElement( "name" );
        name.setNodeValue( AJ_BUILDER );
        buildCommand.appendChild( name );

        // create & append <arguments/>
        buildCommand.appendChild( document.createElement( "arguments" ) );

        buildSpec.insertBefore( buildCommand, buildSpec.getFirstChild() );

        return true;
    }

    private boolean mergeNatures( Document document )
            throws MojoExecutionException
    {
        NodeList naturesList = document.getElementsByTagName( "natures" );
        for( int i = 0; i < naturesList.getLength(); i++ )
        {
            Element natures = (Element) naturesList.item( i );
            NodeList natureList = natures.getElementsByTagName( "nature" );
            for( int j = 0; j < natureList.getLength(); j++ )
            {
                Element nature = (Element) natureList.item( j );
                if( nature.getNodeValue().equals( AJ_NATURE ) )
                {
                    return false;
                }
            }
        }
        Element natures = (Element) naturesList.item( 0 );
        Element nature = document.createElement( "nature" );
        nature.setNodeValue( AJ_NATURE );
        natures.appendChild( nature );
        return true;
    }

    /**
     * write document to the file
     * 
     * @param document
     * @param file 
     * @throws TransformerException
     * @throws FileNotFoundException
     */
    private void writeDocument( Document document, File file )
            throws TransformerException, FileNotFoundException
    {
        document.normalize();
        DOMSource source = new DOMSource( document );
        StreamResult result = new StreamResult( new FileOutputStream( file ) );
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform( source, result );
    }
}
