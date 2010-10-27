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

import org.apache.maven.plugin.logging.Log;
import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageHandler;

/**
 * A Ajc message handler. gets all compiler messages from the ajc compiler, and uses maven plugin logger to print them.
 * 
 * @author <a href="mailto:kaare.nilsen@gmail.com">Kaare Nilsen</a>
 */
public class MavenMessageHandler
    extends MessageHandler
{
    Log log;

    /**
     * Constructs a MessageHandler with a Maven plugin logger.
     * 
     * @param log
     */
    public MavenMessageHandler( Log log )
    {
        super();
        this.log = log;
    }

    /**
     * Hook into the maven logger.
     */
    public boolean handleMessage( IMessage message )
        throws AbortException
    {
        if ( message.getKind().equals( IMessage.WARNING ) && !isIgnoring( IMessage.WARNING ) )
        {
            log.warn( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.DEBUG ) && !isIgnoring( IMessage.DEBUG ) )
        {
            log.debug( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.ERROR ) && !isIgnoring( IMessage.ERROR ) )
        {
            log.error( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.ABORT ) && !isIgnoring( IMessage.ABORT ) )
        {
            log.error( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.FAIL ) && !isIgnoring( IMessage.FAIL ) )
        {
            log.error( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.INFO ) && !isIgnoring( IMessage.INFO ))
        {
            log.debug( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.WEAVEINFO ) && !isIgnoring( IMessage.WEAVEINFO ) )
        {
            log.info( (CharSequence) message.getMessage() );
        }
        else if ( message.getKind().equals( IMessage.TASKTAG ) && !isIgnoring( IMessage.TASKTAG ) )
        {
            log.debug( (CharSequence) message.getMessage() );
        }
        return super.handleMessage( message );
    }
}
