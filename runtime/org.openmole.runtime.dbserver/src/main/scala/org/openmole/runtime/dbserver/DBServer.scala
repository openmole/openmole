/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.runtime.dbserver

import com.db4o.cs.Db4oClientServer
import com.db4o.defragment.Defragment
import com.db4o.defragment.DefragmentConfig
import com.db4o.ta.TransparentPersistenceSupport
import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import org.openmole.misc.replication.DBServerInfo
import org.openmole.misc.replication.Replica

object DBServer extends App {

  val user = UUID.randomUUID.toString
  val password = UUID.randomUUID.toString

  val base = DBServerInfo.base

  private def dB4oConfiguration = {
    val configuration = Db4oClientServer.newServerConfiguration
    configuration.common.add(new TransparentPersistenceSupport)
    configuration.common.objectClass(classOf[Replica]).cascadeOnDelete(true)
    configuration.common.bTreeNodeSize(256)
    configuration.common.objectClass(classOf[Replica]).objectField("_hash").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_source").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_storageDescription").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_authenticationKey").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_destination").indexed(true)
    configuration.file.lockDatabaseFile(false)
    configuration
  }

  def defrag(db: File) = {
    val defragmentConfig = new DefragmentConfig(db.getAbsolutePath)
    defragmentConfig.forceBackupDelete(true)
    Defragment.defrag(defragmentConfig)
  }

  def open(dbFile: File) = Db4oClientServer.openServer(dB4oConfiguration, dbFile.getAbsolutePath, -1)

  val lockFile = DBServerInfo.dbLockFile(base)
  lockFile.createNewFile

  val str = new FileOutputStream(lockFile)
  val lock = str.getChannel.tryLock

  if (lock != null) {
    val objRepo = DBServerInfo.dbFile(base)
    if (objRepo.exists) defrag(objRepo)

    val server = open(objRepo)

    Runtime.getRuntime.addShutdownHook(
      new Thread {
        override def run = {
          lock.release
          str.close
          server.close
        }
      })

    server.grantAccess(user, password)
    val serverInfo = new DBServerInfo(server.ext.port, user, password)
    val dbInfoFile = DBServerInfo.dbInfoFile(base)
    dbInfoFile.deleteOnExit
    val out = new FileOutputStream(dbInfoFile)
    try new XStream().toXML(serverInfo, out) finally out.close
    server.openClient.close

    Thread.sleep(Long.MaxValue)
  } else println("Server is allready running")

}
