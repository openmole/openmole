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

import gridscale.egi._
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.fileservice.FileService
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
//
//trait BDIIStorageServers extends BatchEnvironment { env ⇒
//  import services._
//
//  type SS = EGIStorageService
//
//  def bdiis: Seq[BDIIServer]
//  def voName: String
//  def debug: Boolean
//  def proxyCreator: () ⇒ GlobusAuthentication.Proxy
//
//  def storages = _storages()
//  val _storages = Cache {
//    val webdavStorages = findWorking(bdiis, (b: BDII) ⇒ b.queryWebDAVLocations(voName))
//    if (!webdavStorages.isEmpty) {
//      logger.fine("Use webdav storages:" + webdavStorages.mkString(","))
//      implicit def preference = services.preference
//      webdavStorages.map { s ⇒ EGIWebDAVStorageService(s, env, voName, debug, proxyCreator) }
//    }
//    else throw new UserBadDataError("No WebDAV storage available for the VO")
//  }
//
//  def trySelectAStorage(usedFiles: Vector[File]) = {
//    import EGIEnvironment._
//
//    val sss = storages
//    if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")
//
//    val nonEmpty = sss.filter(!_.usageControl.isEmpty)
//
//    case class FileInfo(size: Long, hash: String)
//
//    val usedFilesInfo = usedFiles.map { f ⇒ f → FileInfo(f.size, fileService.hash(f).toString) }.toMap
//    val totalFileSize = usedFilesInfo.values.toSeq.map(_.size).sum
//    val onStorage = replicaCatalog.forHashes(usedFilesInfo.values.toVector.map(_.hash), sss.map(_.id)).groupBy(_.storage)
//
//    def minOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.min)
//    def maxOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.max)
//
//    val times = sss.flatMap(_.usageControl.time)
//    val maxTime = maxOption(times)
//    val minTime = minOption(times)
//
//    val availablities = nonEmpty.flatMap(_.usageControl.availability)
//    val maxAvailability = maxOption(availablities)
//    val minAvailability = minOption(availablities)
//
//    def rate(ss: EGIStorageService) = {
//      val sizesOnStorage = usedFilesInfo.filter { case (_, info) ⇒ onStorage.getOrElse(ss.id, Set.empty).exists(_.hash == info.hash) }.values.map { _.size }
//      val sizeOnStorage = sizesOnStorage.sum
//
//      val sizeFactor = if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0
//
//      val timeFactor =
//        (minTime, maxTime, ss.usageControl.time) match {
//          case (Some(minTime), Some(maxTime), Some(time)) if (maxTime > minTime) ⇒ 0.0 - time.normalize(minTime, maxTime)
//          case _ ⇒ 0.0
//        }
//
//      val availabilityFactor =
//        (minAvailability, maxAvailability, ss.usageControl.availability) match {
//          case (Some(minAvailability), Some(maxAvailability), Some(availability)) if (maxAvailability > minAvailability) ⇒ 0.0 - availability.normalize(minAvailability, maxAvailability)
//          case _ ⇒ 0.0
//        }
//
//      math.pow(
//        preference(StorageSizeFactor) * sizeFactor +
//          preference(StorageTimeFactor) * timeFactor +
//          preference(StorageAvailabilityFactor) * availabilityFactor +
//          preference(StorageSuccessRateFactor) * ss.usageControl.successRate.getOrElse(0.0),
//        preference(StorageFitnessPower)
//      )
//    }
//
//    implicit def preference = services.preference
//    select(sss.toList, rate)
//  }
//
//}
