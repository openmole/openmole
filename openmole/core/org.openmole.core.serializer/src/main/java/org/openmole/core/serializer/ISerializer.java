/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.function.IPartialFunction;
import org.openmole.commons.tools.io.IHash;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public interface ISerializer {

  Tuple2<Map<File, IHash>, Collection<Class>> serializeFilePathAsHashGetPluginClassAndFiles(Object obj, File file) throws InternalProcessingError, UserBadDataError;
  
  Tuple2<Collection<File>, Collection<Class>> serializeGetPluginClassAndFiles(Object obj, File file) throws InternalProcessingError;
  Tuple2<Collection<File>, Collection<Class>> serializeGetPluginClassAndFiles(Object obj, OutputStream os) throws InternalProcessingError;    
  
  <T> T deserializeReplacePathHash(File file, IPartialFunction<IHash, File> files)  throws InternalProcessingError;
  <T> T deserializeReplacePathHash(InputStream it, IPartialFunction<IHash, File> files) throws InternalProcessingError;

  <T> T deserializeReplaceFiles(File file, IPartialFunction<File,File> files)  throws InternalProcessingError;
  <T> T deserializeReplaceFiles(InputStream it, IPartialFunction<File, File> files) throws InternalProcessingError;
 
  <T> T deserialize(File file) throws InternalProcessingError;
  <T> T deserialize(InputStream is) throws InternalProcessingError;

  void serialize(Object obj, File file) throws InternalProcessingError;
  void serialize(Object obj, OutputStream os) throws InternalProcessingError;
    
  void serializeAsHash(Object obj, File file) throws InternalProcessingError, UserBadDataError;
}
