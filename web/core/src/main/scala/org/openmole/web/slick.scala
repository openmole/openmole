package org.openmole.web

import org.scalatra.ScalatraServlet

import org.slf4j.LoggerFactory

import slick.driver.H2Driver.simple._
import com.jolbox.bonecp._

//import scala.slick.session.Database
import Database.threadLocalSession
import java.io.IOException

/*object Workflows extends Table[(Int, String, UUID)]("WORKFLOWS") {
  def id = column[Int]("WF_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("WF_NAME")
  def version = column[String]("WF_VERSION")
  def uuid = column[UUID]("WF_FOLDERUUID")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id ~ name ~ version ~ uuid
}

object Tags extends Table[(Int, String)]("TAGS") {
  def id = column[Int]("TAG_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("TAG_NAME")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = id ~ name
}

object WFTag extends Table[(Int, Int)]("WFTag") {
  def wfId = column[Int]("WF_ID")
  def tagId = column[Int]("TAG_ID")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = id ~ name
  def workflow = foreignKey("WF_FK", wfiD, Workflows)(_.id)
  def tag = foreignKey("TAG_FK", tagiD, Tags)(_.id)
}          */

// Definition of the SUPPLIERS table
object Suppliers extends Table[(Int, String, String, String, String, String)]("SUPPLIERS") {
  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("SUP_NAME")
  def street = column[String]("STREET")
  def city = column[String]("CITY")
  def state = column[String]("STATE")
  def zip = column[String]("ZIP")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id ~ name ~ street ~ city ~ state ~ zip
}

// Definition of the COFFEES table
object Coffees extends Table[(String, Int, Double, Int, Int)]("COFFEES") {
  def name = column[String]("COF_NAME", O.PrimaryKey)
  def supID = column[Int]("SUP_ID")
  def price = column[Double]("PRICE")
  def sales = column[Int]("SALES")
  def total = column[Int]("TOTAL")
  def * = name ~ supID ~ price ~ sales ~ total

  // A reified foreign key relation that can be navigated to create a join
  def supplier = foreignKey("SUP_FK", supID, Suppliers)(_.id)
}

trait SlickSupport extends ScalatraServlet {

  val logger = LoggerFactory.getLogger(getClass)

  try {
    Class.forName("org.h2.Driver")
  } catch {
    case e: ClassNotFoundException ⇒ println("Suffered irrecoverable error: " + e)
  }

  var connectionPool = {
    val boneCfg = new BoneCPConfig()
    boneCfg.setJdbcUrl("jdbc:h2:~/tmp/test;TRACE_LEVEL_FILE=4;MVCC=TRUE")
    boneCfg.setUser("root")
    boneCfg.setPassword("")
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

  //val db = Database.forURL("jdbc:h2:~/test", "root", "")

  override def destroy() {
    super.destroy()
    closeDbConnection()
  }
}

class SlickRoutes extends ScalatraServlet with SlickSupport {

  get("/db/create-tables") {
    db withSession {
      (Suppliers.ddl ++ Coffees.ddl).create
    }
  }

  get("/db/load-data") {
    db withSession {
      // Insert some suppliers
      Suppliers.insert(101, "Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199")
      Suppliers.insert(49, "Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460")
      Suppliers.insert(150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966")

      // Insert some coffees (using JDBC's batch insert feature, if supported by the DB)
      Coffees.insertAll(
        ("Colombian", 101, 7.99, 0, 0),
        ("French_Roast", 49, 8.99, 0, 0),
        ("Espresso", 150, 9.99, 0, 0),
        ("Colombian_Decaf", 101, 8.99, 0, 0),
        ("French_Roast_Decaf", 49, 9.99, 0, 0))
    }
  }

  get("/db/drop-tables") {
    db withSession {
      (Suppliers.ddl ++ Coffees.ddl).drop
    }
  }

  get("/coffees") {
    db withSession {
      val q3 = for {
        c ← Coffees
        s ← c.supplier
      } yield (c.name.asColumnOf[String], s.name.asColumnOf[String])

      contentType = "text/html"
      q3.list.map { case (s1, s2) ⇒ "  " + s1 + " supplied by " + s2 } mkString "<br />"
    }
  }

}

