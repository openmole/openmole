package org.openmole.core

import java.io.File

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.{ NoTypePermission }
import squants.time.Time

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.io.Source
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

import java.sql.DriverManager


import scala.util.{Try, Failure, Success}
import java.sql.{Connection, Statement, DriverManager}
import scala.collection.mutable.ListBuffer
import java.sql.ResultSet
import org.openmole.core.db.Transactor.AlreadyInDb
import org.openmole.core.db.Transactor.Inserted
import java.sql.PreparedStatement


package object db extends JavaLogger {

  type Database = Transactor

  case class DBServerInfo(port: Int, user: String, password: String)

  object Transactor:
    sealed trait InsertionResult
    case class AlreadyInDb(replica: Replica) extends InsertionResult
    case class Inserted(replica: Replica) extends InsertionResult
    
    def unrollResut(rs: ResultSet) = 
      def replica =
        Replica(
          id = rs.getLong(1),
          source = rs.getString(2),
          storage = rs.getString(3),
          path = rs.getString(4),
          hash = rs.getString(5),
          lastCheckExists = rs.getLong(6)
        )

      val res = ListBuffer[Replica]()
      while(rs.next) res += replica
      res.toVector

  class Transactor(connection: Connection) {
    def close() = 
      connection.close

    def transact[T](f: => T): T = synchronized {
      val res = 
        try 
          val res: T = f
          res
        catch 
          case e: Throwable =>
            connection.rollback
            throw e

      connection.commit 
      res
    }
    
    lazy val inserQuery = 
      connection.prepareStatement("INSERT INTO REPLICAS (source, storage, path, hash, last_check_exists) values (?,?,?,?,?)")

    lazy val selectOnStorageQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE STORAGE LIKE ?")

    lazy val selectPathQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE PATH LIKE ?")

    lazy val selectHashQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE HASH LIKE ?")

    lazy val selectIdQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE ID = ?")
    
    lazy val deleteIdQuery = 
      connection.prepareStatement("DELETE FROM REPLICAS WHERE ID = ?")

    lazy val deleteOnStorageQuery = 
      connection.prepareStatement("DELETE FROM REPLICAS WHERE STORAGE = ?")

    lazy val selectSameSourceWithDifferentHashQuery =
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE SOURCE LIKE ? AND STORAGE LIKE ? AND HASH NOT LIKE ?")

    lazy val deleteSameSourceWithDifferentHashQuery =
      connection.prepareStatement("DELETE FROM REPLICAS WHERE SOURCE LIKE ? AND STORAGE LIKE ? AND HASH NOT LIKE ?")
    
    lazy val selectSameSourceQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE SOURCE LIKE ? AND STORAGE LIKE ? AND HASH LIKE ?")
    
    lazy val selectSourcesStoragesQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE SOURCE = ANY(?) AND STORAGE = ANY(?)")

    lazy val selectHashesStoragesQuery = 
      connection.prepareStatement("SELECT * FROM REPLICAS WHERE HASH = ANY(?) AND STORAGE = ANY(?)")

    lazy val updateLastCheckExistsQuery = 
      connection.prepareStatement("UPDATE REPLICAS SET LAST_CHECK_EXISTS = ? WHERE ID = ?")

    def insert(source: String, storage: String, path: String, hash: String, lastCheckExists: Long) = transact {
      selectSameSourceQuery.setString(1, source)
      selectSameSourceQuery.setString(2, storage)
      selectSameSourceQuery.setString(3, hash)
      val res = Transactor.unrollResut(selectSameSourceQuery.executeQuery)
      
      if(res.isEmpty) 
      then
        inserQuery.setString(1, source)
        inserQuery.setString(2, storage)
        inserQuery.setString(3, path)
        inserQuery.setString(4, hash)
        inserQuery.setLong(5, lastCheckExists)
        inserQuery.execute()
        val rs = selectSameSourceQuery.executeQuery
        Inserted(Transactor.unrollResut(rs).head)
      else AlreadyInDb(res.head)
    }

    def delete(id: Long) = transact {
      selectIdQuery.setLong(1, id)
      val res = Transactor.unrollResut(selectIdQuery.executeQuery)
      deleteIdQuery.setLong(1, id)
      deleteIdQuery.execute
      res.headOption
    }

    def select(id: Long) = transact {
      selectIdQuery.setLong(1, id)
      Transactor.unrollResut(selectIdQuery.executeQuery)
    }

    def selectOnStorage(s: String) = transact {
      selectOnStorageQuery.setString(1, s)
      Transactor.unrollResut(selectOnStorageQuery.executeQuery)
    }

    def deleteOnStorage(s: String) = transact {
      selectOnStorageQuery.setString(1, s)
      val res = Transactor.unrollResut(selectOnStorageQuery.executeQuery)
      deleteOnStorageQuery.setString(1, s)
      res
    }

    def selectPath(s: String) = transact {
      selectPathQuery.setString(1, s)
      Transactor.unrollResut(selectPathQuery.executeQuery)
    }

    def selectHash(s: String) = transact {
      selectHashQuery.setString(1, s)
      Transactor.unrollResut(selectHashQuery.executeQuery)
    }

    def selectSourcesStorages(sources: Seq[String], storages: Seq[String])  = transact {
       val sourcesArray = connection.createArrayOf("VARCHAR", sources.toArray[Any])
       val storagesArray = connection.createArrayOf("VARCHAR", storages.toArray[Any])
       selectSourcesStoragesQuery.setArray(1, sourcesArray)
       selectSourcesStoragesQuery.setArray(2, storagesArray)
       Transactor.unrollResut(selectSourcesStoragesQuery.executeQuery)
    }

    def selectHashesStorages(sources: Seq[String], storages: Seq[String])  = transact {
       val sourcesArray = connection.createArrayOf("VARCHAR", sources.toArray[Any])
       val storagesArray = connection.createArrayOf("VARCHAR", storages.toArray[Any])
       selectHashesStoragesQuery.setArray(1, sourcesArray)
       selectHashesStoragesQuery.setArray(2, storagesArray)
       Transactor.unrollResut(selectHashesStoragesQuery.executeQuery)
    }

    def selectSameSourceWithDifferentHash(source: String, storage: String, hash: String) = transact {
      selectSameSourceWithDifferentHashQuery.setString(1, source)
      selectSameSourceWithDifferentHashQuery.setString(2, storage)
      selectSameSourceWithDifferentHashQuery.setString(3, hash)
      Transactor.unrollResut(selectSameSourceWithDifferentHashQuery.executeQuery)
    }

    def deleteSameSourceWithDifferentHash(source: String, storage: String, hash: String) = transact {
      selectSameSourceWithDifferentHashQuery.setString(1, source)
      selectSameSourceWithDifferentHashQuery.setString(2, storage)
      selectSameSourceWithDifferentHashQuery.setString(3, hash)
      val res = Transactor.unrollResut(selectSameSourceWithDifferentHashQuery.executeQuery)
      deleteSameSourceWithDifferentHashQuery.setString(1, source)
      deleteSameSourceWithDifferentHashQuery.setString(2, storage)
      deleteSameSourceWithDifferentHashQuery.setString(3, hash)
      deleteSameSourceWithDifferentHashQuery.execute
      res
    }

    def selectSameSource(source: String, storage: String, hash: String) = transact {
      selectSameSourceQuery.setString(1, source)
      selectSameSourceQuery.setString(2, storage)
      selectSameSourceQuery.setString(3, hash)
      Transactor.unrollResut(selectSameSourceQuery.executeQuery)
    }
 
    def updateLastCheckExists(id: Long, lastCheckExists: Long) = transact {
      updateLastCheckExistsQuery.setLong(1, lastCheckExists)
      updateLastCheckExistsQuery.setLong(2, id)
      updateLastCheckExistsQuery.execute
    }

    def selectAll = transact {
      val statement = connection.createStatement
      val rs = statement.executeQuery("SELECT * FROM REPLICAS")
      Transactor.unrollResut(rs)
    }

    def execute(s: String) = transact { 
      val st = connection.createStatement
      try st.execute(s)
      finally st.close
    }

  }

  def dbVersion = 2
  def dbName = s"replica-$dbVersion"
  def dbInfoName = s"$dbName.info"

  def dbDirectory(baseDirectory: File) = baseDirectory / "database"
  def dbFile(dbDirectory: File) = new File(dbDirectory, dbName)
  def dbInfoFile(dbDirectory: File) = new File(dbDirectory, dbInfoName)

  lazy val xstream = {
    val xstream = new XStream()
    xstream.addPermission(NoTypePermission.NONE)
    xstream.allowTypesByWildcard(Array("java.*", "org.openmole.core.*"))
    xstream.setClassLoader(classOf[DBServerInfo].getClassLoader)
    xstream.alias("DBServerInfo", classOf[DBServerInfo])
    xstream
  }

  def load(f: File) = {
    val src = Source.fromFile(f)
    try xstream.fromXML(src.mkString).asInstanceOf[DBServerInfo]
    finally src.close
  }

  def jdbcH2Options = "AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE"

  def insert(transactor: Transactor, source: String, storage: String, path: String, hash: String, lastCheckExists: Long) = 
    transactor.insert(source, storage, path, hash, lastCheckExists)

  def memory() = {
    DriverManager.registerDriver(new org.h2.Driver())
    val connection = DriverManager.getConnection("jdbc:h2:mem:replica")
    connection.setAutoCommit(false)

    val transactor = new Transactor(connection)
    createDB(transactor)
    transactor
  }

  def createDB(transactor: Transactor) = 
    transactor.execute(
      """
      CREATE TABLE IF NOT EXISTS Replicas (
        ID bigint unsigned auto_increment not null,
        SOURCE VARCHAR(255),
        STORAGE VARCHAR(255),
        PATH VARCHAR(255),
        HASH VARCHAR(64),
        LAST_CHECK_EXISTS bigint,
        PRIMARY KEY  (`ID`)
      );
      CREATE INDEX IF NOT EXISTS idx1 ON REPLICAS (SOURCE, HASH, STORAGE);
      CREATE INDEX IF NOT EXISTS idx2 ON REPLICAS (PATH, STORAGE);
      CREATE INDEX IF NOT EXISTS idx3 ON REPLICAS (HASH, STORAGE);
      """
    )

  def databaseServer(baseDirectory: File, lockTimeout: Time) = {
    def dbFile = baseDirectory / dbName
    def urlDBPath = s"jdbc:h2:${dbFile};$jdbcH2Options;"

    DriverManager.registerDriver(new org.h2.Driver())

    val connection = DriverManager.getConnection(urlDBPath)
    connection.setAutoCommit(false)

    val transactor = new Transactor(connection)
    transactor.execute(s"""SET DEFAULT_LOCK_TIMEOUT ${lockTimeout.millis}""")
    createDB(transactor)
    transactor
  }

}
