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

package fr.in2p3.jsaga.adaptor.ssh.data

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
import fr.in2p3.jsaga.adaptor.ssh.SSHAdaptor
import java.io.File
import org.ogf.saga.error._
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.KnownHosts
import ch.ethz.ssh2.SFTPException
import ch.ethz.ssh2.SFTPv3Client
import ch.ethz.ssh2.sftp.AttribPermissions
import ch.ethz.ssh2.sftp.ErrorCodes
import ch.ethz.ssh2.SFTPv3DirectoryEntry

import collection.JavaConversions._

object SFTPDataAdaptor {
  //Other buffer size won't work due to a ssh server bug
  val BUFFER_SIZE = 32768
  
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
  
  
  def makeDir(sftpClient: SFTPv3Client, parentPath: String, directoryName: String, additionalArgs: String = "") = mapExceptions {
    import AttribPermissions._
    val fullPath = parentPath + "/" +  directoryName
    try sftpClient.mkdir(fullPath, S_IRUSR | S_IWUSR | S_IXUSR) 
    catch {
      case e: SFTPException => 
        if(!exists(sftpClient, parentPath, "")) throw new ParentDoesNotExist("Parent entry does not exist: "+ parentPath, e)
        else if(exists(sftpClient, fullPath, "")) throw new AlreadyExistsException("Entry already exists: " + fullPath, e)
        else if(getAttributes(sftpClient, parentPath, "").getType == TYPE_FILE) throw new BadParameterException("Parent entry is a file: " + parentPath)
        else throw new NoSuccessException("Trying to create " + directoryName + " in " + parentPath, e)
    }
  }
  
  def exists(sftpClient: SFTPv3Client, absolutePath: String, additionalArgs: String) = {
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
  
  def getAttributes(sftpClient: SFTPv3Client, absolutePath: String, additionalArgs: String) = 
    mapExceptions(new SFTPFileAttributes(new File(absolutePath).getName, sftpClient.stat(absolutePath)))
  
  def putFromStream(sftpClient: SFTPv3Client, absolutePath: String, append: Boolean, additionalArgs: String, stream: InputStream) = mapExceptions {
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
  
  def getToStream(sftpClient: SFTPv3Client, absolutePath: String, additionalArgs: String, stream: OutputStream) = mapExceptions {
    val fileHandler = sftpClient.openFileRO(absolutePath)
    try {
      val buffer = new Array[Byte](BUFFER_SIZE)
      var cupPos = 0
      Stream.continually(sftpClient.read(fileHandler, cupPos, buffer, 0, BUFFER_SIZE)).takeWhile(_ != -1).foreach{ 
        count => {
          //println("Read " + count + " for " + absolutePath)
          stream.write(buffer, 0, count)
          cupPos += count
        }
      }
    } finally sftpClient.closeFile(fileHandler)
  }
  
  def getHomeDir(sftpClient: SFTPv3Client) = sftpClient.canonicalPath(".")
}

class SFTPDataAdaptor extends SSHAdaptor with FileReaderGetter with FileWriterPutter with DataRename {
             
  import SFTPDataAdaptor._
  import SSHAdaptor._
  
  private var sftpClient: SFTPv3Client = _
  
  override def getType = "sftp"
  
  override def connect(userInfo: String, host: String, port: Int, basePath: String, attributes: java.util.Map[_, _]) = {
    super.connect(userInfo, host, port, basePath, attributes)
    sftpClient = new SFTPv3Client(connection)
  }
 
  override def disconnect = mapExceptions {
    sftpClient.close
    super.disconnect
  }

  override def getToStream(absolutePath: String, additionalArgs: String, stream: OutputStream) = 
    SFTPDataAdaptor.getToStream(sftpClient, absolutePath, additionalArgs, stream)
    
  override def exists(absolutePath: String, additionalArgs: String) = 
    SFTPDataAdaptor.exists(sftpClient, absolutePath, additionalArgs)

  override def getAttributes(absolutePath: String, additionalArgs: String) = 
    SFTPDataAdaptor.getAttributes(sftpClient, absolutePath, additionalArgs)

  override def listAttributes(absolutePath: String, additionalArgs: String) =
    mapExceptions(sftpClient.ls(absolutePath).asInstanceOf[java.util.Vector[SFTPv3DirectoryEntry]].filterNot(e => {e.filename == "." || e.filename == ".."}).map{e => new SFTPFileAttributes(e.filename, e.attributes)}).toArray
 

  override def putFromStream(absolutePath: String, append: Boolean, additionalArgs: String, stream: InputStream) = 
    SFTPDataAdaptor.putFromStream(sftpClient, absolutePath, append, additionalArgs, stream)

  override def makeDir(parentPath: String, directoryName: String, additionalArgs: String) = 
    SFTPDataAdaptor.makeDir(sftpClient, parentPath, directoryName, additionalArgs)
   

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
