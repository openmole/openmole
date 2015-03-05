package org.openmole.web.db

import org.openmole.core.workspace.Workspace
import org.slf4j.LoggerFactory
import com.jolbox.bonecp.{ BoneCPDataSource, BoneCPConfig }
import slick.driver.H2Driver.simple._

/**
 * Created by mhammons on 4/24/14.
 */
class SlickDB(private val pw: String) {
  try {
    Class.forName("org.h2.Driver")
  }
  catch {
    case e: ClassNotFoundException ⇒ println("Suffered irrecoverable error: " + e)
  }

  private val logger = org.openmole.web.Log.log

  private val connectionPool = {
    val boneCfg = new BoneCPConfig()
    val dbFile: java.io.File = Workspace.file("WebserverDB")
    boneCfg.setJdbcUrl(s"jdbc:h2:${dbFile.getCanonicalPath};TRACE_LEVEL_FILE=4;MVCC=TRUE;CIPHER=AES")
    boneCfg.setUser("root")
    boneCfg.setPassword(s"$pw openmole")
    boneCfg.setMinConnectionsPerPartition(5)
    boneCfg.setMaxConnectionsPerPartition(10)
    boneCfg.setPartitionCount(1)
    boneCfg.setDefaultAutoCommit(true)

    new BoneCPDataSource(boneCfg)
  }

  def closeDbConnection() {
    logger.info("Closing boneCP connection pool")
    connectionPool.close
  }

  val db = Database.forDataSource(connectionPool)
}

/*object SlickDB {
  private var initialized = false
  private var pw = ""


  def init(password: String) = {
    pw = password
    initialized = true

  }

  protected val dbPassword: String;
  //val db = Database.forURL("jdbc:h2:~/test", "root", "")
}*/
