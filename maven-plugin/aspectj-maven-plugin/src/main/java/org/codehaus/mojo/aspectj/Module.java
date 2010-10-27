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

/**
 * Module to be referenced through AJC-mojo 
 * 
 * @author <a href="mailto:tel@objectnet.no">Thor �ge Eldby</a>
 */
public class Module
{

    /** Artifact's group id */
    private String groupId;

    /** Artifact's id */
    private String artifactId;

    /**
     * @return id of artifact
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @param artifactId id of artifact
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @return id of artifact's group
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @param groupId id of artifact's group
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String toString()
    {
        return getGroupId() + ":" + getArtifactId();
    }

}
