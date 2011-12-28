/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.batch.file

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.AccessToken

object IURIFile {
  
  implicit def ordering = new Ordering[IURIFile] {
    def compare(left: IURIFile, right: IURIFile): Int = {
      left.location.compareTo(right.location)
    }
  }
  
}

trait IURIFile {
  def isDirectory: Boolean 
  def isDirectory(token: AccessToken): Boolean
  def URLRepresentsADirectory: Boolean
    
  def mkdir(name: String): IURIFile
  def mkdir(name: String, token: AccessToken): IURIFile
    
  def mkdirIfNotExist(name: String): IURIFile
  def mkdirIfNotExist(name: String, token: AccessToken): IURIFile
    
  def newFileInDir(prefix: String, sufix: String): IURIFile
    
  def openInputStream: InputStream 
  def openInputStream(token: AccessToken): InputStream 
    
  def openOutputStream: OutputStream
  def openOutputStream(token: AccessToken): OutputStream
    
  def touch
  def touch(token: AccessToken)
  
  def copy(dest: File)
  def copy(dest: File, srcToken: AccessToken)
  
  //def copy(dest: IURIFile)
  //def copy(dest: IURIFile, srcToken: AccessToken)
    
  def remove(recusrsive: Boolean) 
  def remove(recursive: Boolean, token: AccessToken)   
  
  def remove(timeOut: Boolean, recusrsive: Boolean) 
  def remove(timeOut: Boolean, recusrsive: Boolean, token: AccessToken) 
    
  def list: Iterable[String]
  def list(token: AccessToken): Iterable[String]
    
  def exists: Boolean 
  def exists(token: AccessToken): Boolean 

  def exist(name: String): Boolean 
  def exist(name: String, token: AccessToken): Boolean 

  def name: String
  
  def modificationTime(name: String): Long 
  def modificationTime(name: String, token: AccessToken): Long
    
  def modificationTime: Long 
  def modificationTime(token: AccessToken): Long
    
  def cache: File
  def cache(token: AccessToken): File
    
  def child(child: String): IURIFile
    
  def URI: URI
  def location: String
  def path: String
    
  def storageDescription: ServiceDescription
}
