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

import fr.in2p3.jsaga.adaptor.security.SecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.UserPassSecurityCredential
import fr.in2p3.jsaga.adaptor.security.impl.UserPassStoreSecurityCredential
import fr.in2p3.jsaga.adaptor.base.usage.UAnd
import fr.in2p3.jsaga.adaptor.base.usage.UOptional
import fr.in2p3.jsaga.adaptor.base.usage.Usage
import fr.in2p3.jsaga.adaptor.base.defaults.Default
import fr.in2p3.jsaga.adaptor.ClientAdaptor
import fr.in2p3.jsaga.adaptor.data.optimise.DataRename
import fr.in2p3.jsaga.adaptor.data.read.FileAttributes
import fr.in2p3.jsaga.adaptor.data.read.FileReaderGetter
import fr.in2p3.jsaga.adaptor.data.write.FileWriterPutter
import fr.in2p3.jsaga.adaptor.data.ParentDoesNotExist
import FileAttributes._
 
import java.io.OutputStream
import java.io.InputStream
import java.io.File
import org.ogf.saga.error._
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.KnownHosts
import ch.ethz.ssh2.SFTPException
import ch.ethz.ssh2.SFTPv3Client
import ch.ethz.ssh2.sftp.ErrorCodes
import ch.ethz.ssh2.SFTPv3DirectoryEntry

import collection.JavaConversions._

object SFTPDataAdaptor {
  val COMPRESSION_LEVEL = "CompressionLevel"
  val KNOWN_HOSTS = "KnownHosts"
  val IGNORE_KNOWN_HOSTS = "IgnoreKnownHosts"
  val BUFFER_SIZE = 10240
}

class SFTPDataAdaptor extends ClientAdaptor with FileReaderGetter with FileWriterPutter with DataRename {
             
  import SFTPDataAdaptor._
  
  private var credential: SecurityCredential = null
  private var connection: Connection = null
  private var sftpClient: SFTPv3Client = null
  
  override def getType = "sftp"
  override def getDefaultPort = 22
  
  override def getSupportedSecurityCredentialClasses = Array(classOf[UserPassSecurityCredential], classOf[UserPassStoreSecurityCredential], classOf[SSHSecurityCredential])
  
  override def setSecurityCredential(credential: SecurityCredential) = this.credential = credential;
	
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
   
      sftpClient = new SFTPv3Client(connection)
    } catch {
      case e: Exception => throw new AuthenticationFailedException("Authentication failed.", e)
    }
    
  }

  override def disconnect = mapExceptions {
    sftpClient.close
    connection.close
  }

  override def getToStream(absolutePath: String, additionalArgs: String, stream: OutputStream) = mapExceptions {
      val fileHandler = sftpClient.openFileRO(absolutePath)
      try {
        val buffer = new Array[Byte](BUFFER_SIZE)
        var cupPos = 0
        Stream.continually(sftpClient.read(fileHandler, cupPos, buffer, 0, BUFFER_SIZE)).takeWhile(_ != -1).foreach{ 
          count => {
            stream.write(buffer, 0, count)
            cupPos += count
          }
        }
      } finally sftpClient.closeFile(fileHandler)
  }

  override def exists(absolutePath: String, additionalArgs: String) = {
    try {
      sftpClient.stat(absolutePath)
      true
    } catch {
      case e: SFTPException => 
        if(e.getServerErrorCode == ErrorCodes.SSH_FX_NO_SUCH_FILE) false
        else throw new NoSuccessException(e)
      case e: Exception => throw new NoSuccessException(e)
    }
  }
  
  private def mapExceptions[T](f: => T) = 
    try f
  catch {
    case e: SFTPException => 
      throw e.getServerErrorCode match {
        case ErrorCodes.SSH_FX_NO_SUCH_FILE => new DoesNotExistException(e)
        case ErrorCodes.SSH_FX_PERMISSION_DENIED => new PermissionDeniedException(e)
        case _ => new NoSuccessException(e)
      }
    case e: Exception => throw new NoSuccessException(e)
  }
  

  override def getAttributes(absolutePath: String, additionalArgs: String) = 
    mapExceptions(new SFTPFileAttributes(new File(absolutePath).getName, sftpClient.stat(absolutePath)))


  override def listAttributes(absolutePath: String, additionalArgs: String) =
    mapExceptions(sftpClient.ls(absolutePath).asInstanceOf[java.util.Vector[SFTPv3DirectoryEntry]].filterNot(e => {e.filename == "." || e.filename == ".."}).map{e => new SFTPFileAttributes(e.filename, e.attributes)}).toArray
 

  override def putFromStream(absolutePath: String, append: Boolean, additionalArgs: String, stream: InputStream) = mapExceptions {
      val fileHandler = if(append) sftpClient.createFile(absolutePath) else sftpClient.createFileTruncate(absolutePath)
      try {
        val buffer = new Array[Byte](BUFFER_SIZE)
        var cupPos = if(append) {
          val size = sftpClient.stat(absolutePath).size
          if(size == null) 0L else size.toLong
        } else 0L
          
        Stream.continually(stream.read(buffer, 0, BUFFER_SIZE)).takeWhile(_ != -1).foreach{ 
          count => {
            sftpClient.write(fileHandler, cupPos, buffer, 0, count)
            cupPos += count
          }
        }
      } finally sftpClient.closeFile(fileHandler)
  }

  override def makeDir(parentAbsolutePath: String, directoryName: String, additionalArgs: String) = mapExceptions {
    val fullPath = parentAbsolutePath + "/" +  directoryName
    try sftpClient.mkdir(fullPath, 0x0700) 
    catch {
      case e: SFTPException => 
        if(!exists(parentAbsolutePath, "")) throw new ParentDoesNotExist("Parent entry does not exist: "+ parentAbsolutePath, e)
        else if(exists(fullPath, "")) throw new AlreadyExistsException("Entry already exists: " + fullPath, e)
        else if(getAttributes(parentAbsolutePath, "").getType == TYPE_FILE) throw new BadParameterException("Parent entry is a file: " + parentAbsolutePath)
        else throw e
    }
  }

  override def removeDir(parentAbsolutePath: String, directoryName: String, additionalArgs: String) = mapExceptions {
    val fullPath = parentAbsolutePath + "/" +  directoryName
    try sftpClient.rmdir(fullPath)
    catch {
      case e: SFTPException =>
        getAttributes(parentAbsolutePath, "").getType match {
          case TYPE_FILE => throw new BadParameterException("Entry is a file: " + fullPath)
          case TYPE_LINK => throw new BadParameterException("Entry is a link: " + fullPath)
          case _ => throw e
        }
    }
  }

  override def removeFile(parentAbsolutePath: String, fileName: String, additionalArgs: String) = mapExceptions {
    val fullPath = parentAbsolutePath + "/" +  fileName
    try sftpClient.rm(fullPath)
    catch {
      case e: SFTPException =>
        if(getAttributes(parentAbsolutePath, "").getType == TYPE_DIRECTORY) throw new BadParameterException("Entry is a directory: " + fullPath)
        else throw e
    }
  }

  override def rename(sourceAbsolutePath: String, targetAbsolutePath: String, overwrite: Boolean, additionalArgs: String) = mapExceptions {
    if (overwrite) throw new NoSuccessException("Overwrite not implemented")
    sftpClient.mv(sourceAbsolutePath, targetAbsolutePath)
  }
}
