/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.serializer

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError

trait ISerializer {
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def serializeFilePathAsHashGetPluginClassAndFiles(obj: Object, file: File): (Iterable[(File, FileInfoHash)], Iterable[Class[_]])
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def serializeGetPluginClassAndFiles(obj: Object, file: File): (Iterable[File], Iterable[Class[_]])
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def serializeGetPluginClassAndFiles(obj: Object, os: OutputStream): (Iterable[File], Iterable[Class[_]])    
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserializeReplacePathHash[T](file: File, files: PartialFunction[FileInfoHash, File]): T
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserializeReplacePathHash[T](it: InputStream, files: PartialFunction[FileInfoHash, File]): T

  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserializeReplaceFiles[T](file: File, files: PartialFunction[File, File]): T
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserializeReplaceFiles[T](it: InputStream, files: PartialFunction[File, File]): T
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserialize[T](file: File): T
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def deserialize[T](is: InputStream): T

  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def serialize(obj: Object, file: File)
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def serialize(obj: Object, os: OutputStream)
  
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError]) 
  def serializeAsHash(obj: Object, file: File)
}
