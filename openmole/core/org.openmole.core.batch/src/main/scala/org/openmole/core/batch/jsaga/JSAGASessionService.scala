/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.jsaga

import java.util.Hashtable
import java.util.logging.Level
import java.util.logging.{Logger => JLogger}
import org.ogf.saga.context.Context
import org.ogf.saga.context.ContextFactory
import org.ogf.saga.session.SessionFactory
import org.ogf.saga.session.Session
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace

object JSAGASessionService {
  
  private var sessions = List[(String, Session)]()
  private lazy val defaultSession = SessionFactory.createSession(false)
  private val JSAGAConfigFile = "jsaga-universe.xml"
  private val JSAGATimeOutFile = "jsaga-timeout.properties"
  
  init
  @transient lazy val init = {
    val varDir = Workspace.newDir
    System.setProperty("JSAGA_VAR", varDir.getAbsolutePath)

    System.setProperty("saga.factory", "fr.in2p3.jsaga.impl.SagaFactoryImpl")

    // org.apache.log4j.Logger.getLogger(org.glite.security.util.FileEndingIterator.class.getName()).setLevel(org.apache.log4j.Level.FATAL);
    org.apache.log4j.Logger.getRootLogger.setLevel(org.apache.log4j.Level.FATAL)    
    val universe = this.getClass.getClassLoader.getResource(JSAGAConfigFile)

    if (universe != null)  System.setProperty("jsaga.universe", universe.toString)
    else JLogger.getLogger(JSAGASessionService.getClass.getName).log(Level.WARNING, JSAGAConfigFile + " JSAGA config file not found.");
      
    val timeout = this.getClass.getClassLoader.getResource(JSAGATimeOutFile)

    if (universe != null) System.setProperty("jsaga.timeout", timeout.toString)
    else  JLogger.getLogger(JSAGASessionService.getClass.getName).log(Level.WARNING, JSAGAConfigFile + " JSAGA timeout file not found.")
    
    Unit
  }
    
  def addContext(expr: String, context: Context) = synchronized {
    sessions.filter(_ == expr).headOption match {
      case None => 
        val session = SessionFactory.createSession(false)
        session.addContext(context)
        sessions = (expr -> session) :: sessions
      case Some((pattern,session)) =>
        session.addContext(context)
    }
  }
  
  def session(url: String) = sessions.filter{case(p, s) => url.matches(p)}.headOption match {
    case Some((p, s)) => s
    case None => defaultSession //throw new InternalProcessingError("No session available for url " + url)
  }
  
  def createContext: Context = ContextFactory.createContext
}
