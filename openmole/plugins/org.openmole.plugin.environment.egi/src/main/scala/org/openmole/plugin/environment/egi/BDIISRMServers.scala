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

import java.util.concurrent.TimeUnit

import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ Scaling, Random }
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.egi.{ GlobusAuthentication, BDII }
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.workspace._
import org.openmole.tool.hash.Hash
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import concurrent.stm._
import java.io.File
import Random._
import Scaling._
import scala.annotation.tailrec

object BDIISRMServers extends Logger

trait BDIISRMServers extends BatchEnvironment {
  type SS = EGIStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: GlobusAuthentication.ProxyCreator

  @transient lazy val storages = {
    val bdiiStorarges =
      bdiiServer.querySRMs(voName, Workspace.preferenceAsDuration(EGIEnvironment.FetchResourcesTimeOut))(proxyCreator)

    bdiiStorarges.map {
      s ⇒ EGIStorageService(s, this, proxyCreator)
    }
  }

  def selectAStorage(usedFileHashes: Iterable[(File, Hash)]) =
    storages match {
      case Nil      ⇒ throw new InternalProcessingError("No storage service available for the environment.")
      case s :: Nil ⇒ (s, s.waitAToken)
      case _ ⇒
        val sizes = usedFileHashes.map { case (f, _) ⇒ f -> f.size }.toMap
        val totalFileSize = sizes.values.sum
        val onStorage = ReplicaCatalog.withSession(ReplicaCatalog.inCatalog(_))
        val maxTime = storages.map(_.usageControl.time).max
        val minTime = storages.map(_.usageControl.time).min

        lazy val fitnesses =
          for {
            cur ← storages
            token ← cur.tryGetToken
          } yield {
            val sizeOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(cur.id, Set.empty).contains(h.toString) }.map { case (f, _) ⇒ sizes(f) }.sum

            val sizeFactor =
              if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

            val time = cur.usageControl.time
            val timeFactor =
              if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0 || minTime == maxTime) 0.0
              else 1 - time.normalize(minTime, maxTime)

            import EGIEnvironment._

            val fitness = math.pow(
              Workspace.preferenceAsDouble(StorageSizeFactor) * sizeFactor +
                Workspace.preferenceAsDouble(StorageTimeFactor) * timeFactor +
                Workspace.preferenceAsDouble(StorageAvailabilityFactor) * cur.usageControl.availability +
                Workspace.preferenceAsDouble(StorageSuccessRateFactor) * cur.usageControl.successRate,
              Workspace.preferenceAsDouble(StorageFitnessPower))

            (cur, token, fitness)
          }

        @tailrec def selected(value: Double, storages: List[(StorageService, AccessToken, Double)]): (StorageService, AccessToken) = {
          storages match {
            case Nil                        ⇒ throw new InternalProcessingError("The list should never be empty")
            case (storage, token, _) :: Nil ⇒ (storage, token)
            case (storage, token, fitness) :: tail ⇒
              if (value <= fitness) (storage, token)
              else selected(value - fitness, tail)
          }
        }

        atomic { implicit txn ⇒
          if (!fitnesses.isEmpty) {
            val notLoaded = EGIEnvironment.normalizedFitness(fitnesses).shuffled(Random.default)
            val fitnessSum = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum
            val drawn = Random.default.nextDouble * fitnessSum
            val (storage, token) = selected(drawn, notLoaded.toList)
            for {
              (s, t, _) ← notLoaded
              if s.id != storage.id
            } s.releaseToken(t)

            BDIISRMServers.Log.logger.fine(
              s"""Considered SRM servers:
                 |${fitnesses.map { s ⇒ s._1.toString + " -> " + s._3 }.mkString("\n")}
                 |Selected: $storage
              """.stripMargin)

            storage -> token
          }
          else retry
        }
    }

  def clean = ReplicaCatalog.withSession { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EGIEnvironment.EnvironmentCleaningThreads))
    storages.foreach {
      s ⇒
        background {
          s.withToken { implicit t ⇒ s.clean }
        }(cleaningThreadPool)
    }
    cleaningThreadPool.shutdown()
    cleaningThreadPool.awaitTermination(Long.MaxValue, TimeUnit.DAYS)
  }

}
