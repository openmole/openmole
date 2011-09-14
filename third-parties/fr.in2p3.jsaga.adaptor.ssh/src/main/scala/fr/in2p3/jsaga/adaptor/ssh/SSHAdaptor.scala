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

package fr.in2p3.jsaga.adaptor.ssh

import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.KnownHosts
import fr.in2p3.jsaga.adaptor.ClientAdaptor
import fr.in2p3.jsaga.adaptor.base.defaults.Default
import fr.in2p3.jsaga.adaptor.base.usage.UAnd
import fr.in2p3.jsaga.adaptor.base.usage.UOptional
import fr.in2p3.jsaga.adaptor.base.usage.Usage
import fr.in2p3.jsaga.adaptor.security.SecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.UserPassSecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.UserPassStoreSecurityCredential
import java.io.File
import org.ogf.saga.error.AuthenticationFailedException
import org.ogf.saga.error.BadParameterException

object SSHAdaptor {
  val COMPRESSION_LEVEL = "CompressionLevel"
  val KNOWN_HOSTS = "KnownHosts"
  val IGNORE_KNOWN_HOSTS = "IgnoreKnownHosts"
}

abstract class SSHAdaptor extends ClientAdaptor {
  import SSHAdaptor._
	
  protected var credential: SecurityCredential = _
  protected var connection: Connection = _
  
  override def getSupportedSecurityCredentialClasses = Array(classOf[UserPassSecurityCredential], classOf[UserPassStoreSecurityCredential], classOf[SSHSecurityCredential])
   
  override def setSecurityCredential(credential: SecurityCredential) = this.credential = credential;

  override def getDefaultPort = 22
 	
  override def getUsage = new UAnd(Array[Usage](
      new UOptional(KNOWN_HOSTS),
      new UOptional(IGNORE_KNOWN_HOSTS),
      new UOptional(COMPRESSION_LEVEL)))


  override def getDefaults(map: java.util.Map[_,_]) = 
    Array[Default](new Default(KNOWN_HOSTS, Array[File](new File(System.getProperty("user.home")+"/.ssh/known_hosts"))), new Default(IGNORE_KNOWN_HOSTS, "false"))


  override def connect(userInfo: String, host: String, port: Int, basePath: String, attributes: java.util.Map[_, _]) = {
    try {
      // Creating a connection instance
      connection = new Connection(host, port)
      
      // Now connect
      connection.connect
      
      val ignoreKnowHosts =  if (attributes.containsKey(IGNORE_KNOWN_HOSTS)) attributes.get(IGNORE_KNOWN_HOSTS).asInstanceOf[String].equalsIgnoreCase("true") else false
      
      if(!ignoreKnowHosts) {
        val knownHosts = new KnownHosts
        // Load known_hosts file into in-memory KnownHosts
        if (attributes.containsKey(KNOWN_HOSTS)) {
          val knownHostsFile = new File(attributes.get(KNOWN_HOSTS).asInstanceOf[String])
          if (!knownHostsFile.exists) throw new BadParameterException("Unable to find the selected known host file.")
          knownHosts.addHostkeys(knownHostsFile)
        }
      
        val info = connection.getConnectionInfo
        if(knownHosts.verifyHostkey(host + ':' + port, info.serverHostKeyAlgorithm, info.serverHostKey) ==  KnownHosts.HOSTKEY_HAS_CHANGED) throw new AuthenticationFailedException("Remote host key has changed.")
      }
      
      val isAuthenticated = credential match {
        case credential: UserPassSecurityCredential =>
          val userId = credential.getUserID
          val password = credential.getUserPass
          connection.authenticateWithPassword(userId, password)
        case credential: UserPassStoreSecurityCredential =>
          val userId = credential.getUserID(host)
	  val password = credential.getUserPass(host)
	  connection.authenticateWithPassword(userId, password)
        case credential: SSHSecurityCredential =>
          val userId = credential.getUserID
          val passPhrase = credential.getUserPass
          val key = credential.getPrivateKeyFile
          connection.authenticateWithPublicKey(userId, key, passPhrase)
        case _ => throw new AuthenticationFailedException("Invalid security instance.")
      }
      if (isAuthenticated == false) throw new AuthenticationFailedException("Authentication failed.")
   
    } catch {
      case e: Exception => throw new AuthenticationFailedException("Authentication failed.", e)
    }
  }

  override def disconnect = connection.close

}
