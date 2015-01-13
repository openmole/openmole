package org.openmole.web.cache

import org.openmole.core.workflow.mole.MoleExecution
import java.io.File
import akka.actor.ActorSystem
import org.openmole.web.db.SlickDB
import org.openmole.web.db.tables.{ MoleStats, MoleData }
import slick.driver.H2Driver.simple._
import slick.jdbc.meta.MTable

import javax.sql.rowset.serial.SerialBlob

/**
 * Created by mhammons on 4/23/14.
 */
class Cache(system: ActorSystem, database: SlickDB) {
  private val cachedMoles = new DataHandler[String, MoleExecution](system)
  private val capsules = new DataHandler[String, File](system)
  private val moleStats = new DataHandler[String, Stats](system)
  private val mole2CacheId = new DataHandler[MoleExecution, String](system)

  val logger = org.openmole.web.Log.log

  val dataB = database.db

  dataB withSession { implicit session ⇒
    if (MTable.getTables("MoleData").list.isEmpty)
      MoleData.instance.ddl.create // check that table exists somehow
  }

  dataB withSession { implicit session ⇒
    if (MTable.getTables("MoleStats").list.isEmpty)
      MoleStats.instance.ddl.create
  }

  def cacheMoleExecution(moleExecution: MoleExecution, path: Option[File], cacheId: String) = {
    path foreach (capsules.add(cacheId, _))
    cachedMoles add (cacheId, moleExecution)
    mole2CacheId add (moleExecution, cacheId)
    moleExecution
  }

  def getMole(key: String) = cachedMoles get key

  def getCacheId(mole: MoleExecution) = mole2CacheId get mole getOrElse (throw new Exception("Cache ID requested for uncached mole."))

  def getMoleStats(key: String): Stats = {
    lazy val stats = getStatus(key) flatMap (s ⇒ dataB withSession { implicit session ⇒
      val status = Status.statuses.find(_.toString == s).getOrElse(throw new Exception("Unknown status on DB"))
      (for (s ← MoleStats.instance if s.id === key) yield s.*).list.map {
        case (_, a, b, c, d, e) ⇒
          Stats(a, b, c, d, e, status)
      }.headOption
    })

    moleStats get key orElse stats getOrElse EmptyStats
  }

  def getUnfinishedMoleKeys = dataB withSession { implicit session ⇒
    (for (m ← MoleData.instance if m.state === "Running") yield m.id.asColumnOf[String]).list
  }

  def getMoleKeys = dataB withSession { implicit session ⇒
    (for {
      m ← MoleData.instance
    } yield m.id.asColumnOf[String]).list
  }

  def getStatus(moleId: String): Option[String] =
    dataB withSession { implicit session ⇒
      (for (m ← MoleData.instance if m.id === moleId) yield m.state).list.headOption
    }

  def decacheMole(mole: MoleExecution) = {
    val mKey = mole2CacheId get mole
    mKey foreach (id ⇒ logger.info(s"decaching mole id: $id"))
    mKey foreach (cachedMoles get _ foreach (_.cancel))
    mKey flatMap (moleStats get _) foreach {
      stats ⇒
        dataB withSession { implicit session ⇒
          MoleStats.instance.filter(_.id === mKey.get)
            .map(x ⇒ x.*)
            .update((mKey.get, stats.ready, stats.running, stats.completed, stats.failed, stats.cancelled))
        }
    }
    mKey foreach (k ⇒ List(cachedMoles, moleStats, capsules) map (_ remove k))
    mole2CacheId remove mole
  }

  def deleteMole(key: String) = {
    getMole(key) map decacheMole

    dataB withSession { implicit session ⇒
      MoleData.instance.filter(_.id === key).delete
      MoleStats.instance.filter(_.id === key).delete
    }

  }

  def setStatus(mole: MoleExecution, status: Status) {
    val moleId = getCacheId(mole)
    dataB withSession { implicit session ⇒
      val x = for { m ← MoleData.instance if m.id === moleId } yield m.state
      x update status.toString

    }
    logger.info(s"updated mole: ${moleId} to ${status}")
  }

  def getMoleResult(key: String) = dataB withSession { implicit session ⇒
    val blob = (for (m ← MoleData.instance if m.id === key) yield m.result).list.head
    blob.getBytes(1, blob.length.toInt)
  }

  def storeResultBlob(exec: MoleExecution, blob: SerialBlob) = dataB withSession { implicit session ⇒
    val moleId = getCacheId(exec)
    val r = for (m ← MoleData.instance if m.id === moleId) yield m.result
    r.update(blob)
    logger.info(s"Blob stored for mole: $moleId")
  }

  def getCapsule(exec: MoleExecution) = mole2CacheId get exec flatMap (capsules get _)

  def updateStats(exec: MoleExecution, stats: Stats) {
    val moleId = getCacheId(exec)
    moleStats add (moleId, stats)
    logger.info(s"Updated the stats of mole $moleId to $stats")
  }

}
