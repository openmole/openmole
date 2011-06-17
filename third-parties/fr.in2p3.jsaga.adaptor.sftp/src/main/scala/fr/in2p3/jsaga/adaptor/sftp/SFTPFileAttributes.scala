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

import ch.ethz.ssh2.SFTPv3FileAttributes
import ch.ethz.ssh2.sftp.AttribPermissions
import fr.in2p3.jsaga.adaptor.data.permission.PermissionBytes
import fr.in2p3.jsaga.adaptor.data.read.FileAttributes

class SFTPFileAttributes(name: String, attributes: SFTPv3FileAttributes) extends FileAttributes {
  
  import FileAttributes._

  override def getName = name

  override def getType = 
    if(attributes.isDirectory) TYPE_DIRECTORY
    else if(attributes.isRegularFile) TYPE_FILE
    else if(attributes.isSymlink) TYPE_LINK
    else TYPE_UNKNOWN

  override def getSize = 
    if(attributes.size == null) SIZE_UNKNOWN
    else attributes.size

  private def has(p: Int) = (attributes.permissions | p) == attributes.permissions
  private def permUnknownOr(perm: => PermissionBytes) = if(attributes.permissions == null) PERMISSION_UNKNOWN else perm
  
  override def getUserPermission = {
    permUnknownOr {
      import AttribPermissions._
      import PermissionBytes._
    
      (if(has(S_IRUSR)) READ else NONE) or (if(has(S_IWUSR)) WRITE else NONE) or (if(has(S_IXUSR)) EXEC else NONE)
    }
  }

  override def getGroupPermission = {
    permUnknownOr {
      import AttribPermissions._
      import PermissionBytes._
    
      (if(has(S_IRGRP)) READ else NONE) or (if(has(S_IWGRP)) WRITE else NONE) or (if(has(S_IXGRP)) EXEC else NONE)
    }
  }

  override def getAnyPermission= {
    permUnknownOr {
      import AttribPermissions._
      import PermissionBytes._
    
      (if(has(S_IROTH)) READ else NONE) or (if(has(S_IWOTH)) WRITE else NONE) or (if(has(S_IXOTH)) EXEC else NONE)
    }
  }
    
  override def getOwner = if(attributes.uid == null) ID_UNKNOWN else attributes.uid.toString

  override def getGroup = if(attributes.gid == null) ID_UNKNOWN else attributes.gid.toString

  //FIXME this will stop working in 2033
  override def getLastModified = if(attributes.mtime == null) DATE_UNKNOWN else attributes.mtime.toLong
}
