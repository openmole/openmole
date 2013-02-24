package org.openmole.web

import org.scalatra.ScalatraServlet

import com.mchange.v2.c3p0.ComboPooledDataSource

import java.util.Properties
import org.slf4j.LoggerFactory

import slick.driver.H2Driver.simple._

//import scala.slick.session.Database
import Database.threadLocalSession
import java.io.IOException

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

  val cpds = {
    val props = new Properties
    try {
      props.load(getClass().getClassLoader().getResourceAsStream("/c3p0-config.properties"))
    } catch {
      case e: IOException ⇒ println("error when load propertie file " + e)
    }

    println("props driverclass = " + props.getProperty("c3p0.driverClass"))

    val cpds = new ComboPooledDataSource
    cpds.setProperties(props)
    logger.info("Created c3p0 connection pool")
    cpds
  }

  def closeDbConnection() {
    logger.info("Closing c3po connection pool")
    cpds.close
  }

  val db = Database.forDataSource(cpds)

  override def destroy() {
    super.destroy()
    closeDbConnection
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

