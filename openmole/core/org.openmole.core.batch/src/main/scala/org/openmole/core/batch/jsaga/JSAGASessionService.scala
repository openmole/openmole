/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.jsaga

import java.util.Hashtable
import java.util.logging.Level
import java.util.logging.Logger
import org.ogf.saga.context.Context
import org.ogf.saga.context.ContextFactory
import org.ogf.saga.session.SessionFactory
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.workspace.Workspace
import org.osgi.framework.BundleContext
import org.osgi.service.url.URLConstants
import org.osgi.service.url.URLStreamHandlerService
import org.openmole.core.batch.internal.Activator

object JSAGASessionService {
  private val JSAGAConfigFile = "jsaga-universe.xml"
  private val JSAGATimeOutFile = "jsaga-timeout.properties"
 
  init
  
  private def init = {
    System.setProperty("JSAGA_VAR", Workspace.newDir.getAbsolutePath)
    System.setProperty("saga.factory", "fr.in2p3.jsaga.impl.SagaFactoryImpl")

    // org.apache.log4j.Logger.getLogger(org.glite.security.util.FileEndingIterator.class.getName()).setLevel(org.apache.log4j.Level.FATAL);
    org.apache.log4j.Logger.getRootLogger.setLevel(org.apache.log4j.Level.FATAL);

    val universe = this.getClass.getClassLoader.getResource(JSAGAConfigFile)

    if (universe != null)  System.setProperty("jsaga.universe", universe.toString)
    else Logger.getLogger(JSAGASessionService.getClass.getName).log(Level.WARNING, JSAGAConfigFile + " JSAGA config file not found.");
      
    val timeout = this.getClass.getClassLoader.getResource(JSAGATimeOutFile)

    if (universe != null) System.setProperty("jsaga.timeout", timeout.toString)
    else  Logger.getLogger(JSAGASessionService.getClass.getName).log(Level.WARNING, JSAGAConfigFile + " JSAGA timeout file not found.");
        
    initializeURLProtocol(Activator.context.getOrElse(throw new InternalProcessingError("Context hasn't been initialized")))
  }
  
  private def initializeURLProtocol(context: BundleContext) = {
    val protocol = "httpg"; //$NON-NLS-1$
    //    URLStreamHandlerService svc = new HttpgURLStreamHandlerService();
    val properties = new Hashtable[String, Array[String]]
    properties.put(URLConstants.URL_HANDLER_PROTOCOL, Array[String](protocol))
    context.registerService(classOf[URLStreamHandlerService].getName,new HttpgURLStreamHandlerService,properties)
  }
  
  lazy val session = SessionFactory.createSession(false)
  
  def addContext(context: Context) = {
    if(session.listContexts.contains(context)) session.removeContext(context)
    session.addContext(context)
  }
  
  def createContext: Context = ContextFactory.createContext
}
