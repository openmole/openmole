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

package fr.in2p3.jsaga.adaptor.sftp

import fr.in2p3.jsaga.adaptor.base.defaults.Default
import fr.in2p3.jsaga.adaptor.base.usage._
import fr.in2p3.jsaga.adaptor.security.SecurityCredential
import fr.in2p3.jsaga.adaptor.security.SecurityAdaptor
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential

import org.ogf.saga.context.Context
import org.ogf.saga.error._

import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.util.Map
import collection.JavaConversions._

object SFTPSecurityAdaptor {
  val USER_PUBLICKEY = "UserPublicKey"
}

class SFTPSecurityAdaptor extends SecurityAdaptor {
  import SFTPSecurityAdaptor._

  override def getType = "SFTP"
  
  override def getSecurityCredentialClass = classOf[SSHSecurityCredential]

  override def getUsage = new UAnd(Array[Usage](
      new UFile(Context.USERKEY),
      new UOptional(USER_PUBLICKEY),
      new U(Context.USERID),
      new UOptional(Context.USERPASS)))

  override def getDefaults(map: Map[_,_]) = 
    Array[Default](
      new Default(Context.USERKEY, Array[File](
          new File(System.getProperty("user.home")+"/.ssh/id_rsa"),
          new File(System.getProperty("user.home")+"/.ssh/id_dsa"))), 
      new Default(USER_PUBLICKEY, Array[File](
          new File(System.getProperty("user.home")+"/.ssh/id_rsa.pub"),
          new File(System.getProperty("user.home")+"/.ssh/id_dsa.pub"))),
      new Default(Context.USERID, System.getProperty("user.name"))
    )
    
override def createSecurityCredential(usage: Int, attributes: Map[_,_], contextId: String) = {
  try {
    // load private key
    val privateKeyPath = attributes.get(Context.USERKEY).asInstanceOf[String]
    val publicKeyPath = if (attributes.containsKey(USER_PUBLICKEY)) attributes.get(USER_PUBLICKEY).asInstanceOf[String] else null
			
    // get UserPass
    val userPass = if (attributes.containsKey(Context.USERPASS)) attributes.get(Context.USERPASS).asInstanceOf[String] else null
						
    new SSHSecurityCredential(privateKeyPath, publicKeyPath, userPass, attributes.get(Context.USERID).asInstanceOf[String])
  } catch {
    case e: Exception => throw new NoSuccessException(e)
  }
}
}
