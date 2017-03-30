/*
 * Copyright (C) 10/06/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.io.File

import fr.iscpif.gridscale.egi.{ BDII, GlobusAuthentication }
import fr.iscpif.gridscale.tools.findWorking
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.tools.math._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.cache.Cache
import org.openmole.tool.file._
import org.openmole.tool.hash.Hash
import org.openmole.tool.logger.Logger

object BDIIStorageServers extends Logger

import org.openmole.plugin.environment.egi.BDIIStorageServers.Log._

trait BDIIStorageServers extends BatchEnvironment { env ⇒
  import services._

  implicit val preferences = services.preference
  implicit val threadProvider = services.threadProvider

  type SS = EGIStorageService

  def bdiis: Seq[BDII]
  def voName: String
  def debug: Boolean
  def proxyCreator: () ⇒ GlobusAuthentication.Proxy

  def storages = _storages()
  val _storages = Cache {
    val webdavStorages = findWorking(bdiis, (b: BDII) ⇒ b.queryWebDAVLocations(voName))
    if (!webdavStorages.isEmpty) {
      logger.fine("Use webdav storages:" + webdavStorages.mkString(","))
      webdavStorages.map { s ⇒ EGIWebDAVStorageService(s, env, voName, debug, proxyCreator) }
    }
    else throw new UserBadDataError("No WebDAV storage available for the VO")
  }

  def trySelectAStorage(usedFileHashes: Iterable[(File, Hash)]) = {
    import EGIEnvironment._

    val sss = storages
    if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")

    val nonEmpty = sss.filter(!_.usageControl.isEmpty)
    lazy val sizes = usedFileHashes.map { case (f, _) ⇒ f → f.size }.toMap
    lazy val totalFileSize = sizes.values.sum

    lazy val onStorage = replicaCatalog.inCatalog
    lazy val maxTime = nonEmpty.map(_.usageControl.time).max
    lazy val minTime = nonEmpty.map(_.usageControl.time).min

    lazy val availablities = nonEmpty.map(_.usageControl.availability)
    lazy val maxAvailability = availablities.max
    lazy val minAvailability = availablities.min

    def rate(ss: EGIStorageService) = {
      val sizesOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(ss.id, Set.empty).contains(h.toString) }.map { case (f, _) ⇒ sizes(f) }
      val sizeOnStorage = sizesOnStorage.sum

      val sizeFactor = if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

      val time = ss.usageControl.time
      val timeFactor = if (minTime == maxTime) 1.0 else 1.0 - time.normalize(minTime, maxTime)

      val availability = ss.usageControl.availability
      val availabilityFactor = if (minAvailability == maxAvailability) 1.0 else 1.0 - availability.normalize(minTime, maxTime)

      math.pow(
        preference(StorageSizeFactor) * sizeFactor +
          preference(StorageTimeFactor) * timeFactor +
          preference(StorageAvailabilityFactor) * availabilityFactor +
          preference(StorageSuccessRateFactor) * ss.usageControl.successRate,
        preference(StorageFitnessPower)
      )
    }

    select(sss.toList, rate)
  }

}
