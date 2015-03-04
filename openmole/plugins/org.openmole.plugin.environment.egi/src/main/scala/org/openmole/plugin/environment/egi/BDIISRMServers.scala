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

import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.{ Hash, Scaling, Random }
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.glite.{ GlobusAuthentication, BDII }
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.workspace.Workspace
import concurrent.stm._
import java.io.File
import org.openmole.core.tools.service.Hash
import Random._
import FileUtil._
import Scaling._
import scala.annotation.tailrec

trait BDIISRMServers extends BatchEnvironment {
  type SS = EGIStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: GlobusAuthentication.ProxyCreator

  @transient lazy val threadsBySE = Workspace.preferenceAsInt(EGIEnvironment.LocalThreadsBySE)

  lazy val bdiiStorarges =
    bdiiServer.querySRMs(voName, Workspace.preferenceAsDuration(EGIEnvironment.FetchResourcesTimeOut))(proxyCreator)

  override def allStorages =
    bdiiStorarges.map {
      s ⇒ EGIStorageService(s, this, proxyCreator, threadsBySE)
    }

  override def selectAStorage(usedFileHashes: Iterable[(File, Hash)]) =
    if (storages.size == 1) super.selectAStorage(usedFileHashes)
    else {
      val sizes = usedFileHashes.map { case (f, _) ⇒ f -> f.size }.toMap
      val totalFileSize = sizes.values.sum
      val onStorage = ReplicaCatalog.withSession(ReplicaCatalog.inCatalog(_))
      val maxTime = storages.map(_.time).max
      val minTime = storages.map(_.time).min

      def fitness =
        for {
          cur ← storages
          token ← cur.tryGetToken
        } yield {
          val sizeOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(cur.id, Set.empty).contains(h.toString) }.map { case (f, _) ⇒ sizes(f) }.sum

          val sizeFactor =
            if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

          val time = cur.time
          val timeFactor =
            if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0) 0.0
            else 1 - time.normalize(minTime, maxTime)

          import EGIEnvironment._
          import Workspace.preferenceAsDouble

          val fitness = math.pow(
            preferenceAsDouble(StorageSizeFactor) * sizeFactor +
              preferenceAsDouble(StorageTimeFactor) * timeFactor +
              preferenceAsDouble(StorageAvailabilityFactor) * cur.availability +
              preferenceAsDouble(StorageSuccessRateFactor) * cur.successRate,
            preferenceAsDouble(StorageFitnessPower))

          (cur, token, fitness)
        }

      @tailrec def selected(value: Double, storages: List[(StorageService, AccessToken, Double)]): (StorageService, AccessToken) = {
        storages match {
          case (storage, token, _) :: Nil ⇒ (storage, token)
          case (storage, token, fitness) :: tail ⇒
            if (value <= fitness) (storage, token)
            else selected(value - fitness, tail)
        }
      }

      atomic { implicit txn ⇒
        val fitenesses = fitness
        if (!fitenesses.isEmpty) {
          val notLoaded = EGIEnvironment.normalizedFitness(fitenesses).shuffled(Random.default)
          val fitnessSum = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum
          val drawn = Random.default.nextDouble * fitnessSum
          val (storage, token) = selected(drawn, notLoaded.toList)
          for {
            (s, t, _) ← notLoaded
            if s.id != storage.id
          } s.releaseToken(t)
          storage -> token
        }
        else retry
      }

    }

}
