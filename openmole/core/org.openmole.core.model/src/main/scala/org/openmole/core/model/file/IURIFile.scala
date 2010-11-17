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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.file

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import org.openmole.core.model.execution.batch.BatchServiceDescription
import org.openmole.core.model.execution.batch.IAccessToken

object IURIFile {
  implicit def ordering = new Ordering[IURIFile] {
    def compare(left: IURIFile, right: IURIFile): Int = {
        left.location.compareTo(right.location)
    }
  }
}

trait IURIFile {
    def isDirectory: Boolean 
    def isDirectory(token: IAccessToken): Boolean
    def URLRepresentsADirectory: Boolean
    
    def mkdir(name: String): IURIFile
    def mkdir(name: String, token: IAccessToken): IURIFile
    
    def mkdirIfNotExist(name: String): IURIFile
    def mkdirIfNotExist(name: String, token: IAccessToken): IURIFile
    
    def newFileInDir(prefix: String, sufix: String): IURIFile
    
    def openInputStream: InputStream 
    def openInputStream(token: IAccessToken): InputStream 
    
    def openOutputStream: OutputStream
    def openOutputStream(token: IAccessToken): OutputStream
    
    def copy(dest: IURIFile)
    def copy(dest: IURIFile, srcToken: IAccessToken)
    
    def remove(recusrsive: Boolean) 
    def remove(recursive: Boolean, token: IAccessToken)   
  
    def remove(timeOut: Boolean, recusrsive: Boolean) 
    def remove(timeOut: Boolean, recusrsive: Boolean, token: IAccessToken) 
    
    def list: Iterable[String]
    def list(token: IAccessToken): Iterable[String]
    
    def exist(name: String): Boolean 
    def exist(name: String, token: IAccessToken): Boolean 
    
    def cache: File
    def cache(token: IAccessToken): File
    
    def child(child: String): IURIFile
    
    def URI: URI
    def location: String

    def storageDescription: BatchServiceDescription
}
