package org.codehaus.mojo.aspectj;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.aspectj.bridge.IMessage;

/**
 * Exception thrown when Ajc finds errors during compilation.
 *  
 * @author Carlos Sanchez <carlos@apache.org>
 */
public class CompilationFailedException extends MojoExecutionException
{
    /**
     * 
     */
    private static final long serialVersionUID = 2558168648061612263L;
    private IMessage[] errors;

    public CompilationFailedException( IMessage[] errors )
    {
        super( composeMessage( errors ) );
        this.errors = errors;
    }

    public IMessage[] getErrors()
    {
        return errors;
    }

    private static final String composeMessage( IMessage[] errors ) {
        StringBuffer sb = new StringBuffer();

        sb.append( "Compiler errors:" + SystemUtils.LINE_SEPARATOR );
        for ( int i = 0; i < errors.length; i++ )
        {
            sb.append( errors[i].toString() );
            sb.append( SystemUtils.LINE_SEPARATOR );
        }

        return sb.toString();
    }

}
