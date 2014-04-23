package org.openmole.web.cache

import org.openmole.core.model.mole.IMoleExecution
import java.io.File
import akka.actor.ActorSystem
import org.openmole.web.db.SlickDB

/**
 * Created by mhammons on 4/23/14.
 */
class Cache(system: ActorSystem, db: SlickDB) {
  private val cachedMoles = new DataHandler[String, IMoleExecution](system)
  private val capsules = new DataHandler[String, File](system)
  private val moleStats = new DataHandler[String, Status](system)
  private val mole2CacheId = new DataHandler[IMoleExecution, String](system)

  def cacheMoleExecution(moleExecution: IMoleExecution, path: Option[File], cacheId: String) = {
    path foreach (capsules.add(cacheId, _))
    cachedMoles add (cacheId, moleExecution)
    mole2CacheId add (moleExecution, cacheId)
    moleExecution
  }

  def getMoleStats(key: String) = {
    moleStats get

  }

  def decacheMole(mole: IMoleExecution) = {
    val mKey = mole2CacheId get mole
    mKey foreach (id ⇒ println(s"decaching mole id: $id"))
    mKey foreach (cachedMoles get _ foreach (_.cancel))
    mKey foreach (k ⇒ List(cachedMoles, /*moleStats,*/ capsules) map (_ remove k)) //TODO: Commit the mole stats to the db so they can be retrieved after decaching.
    mole2CacheId remove mole
  }
}
