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

package org.openmole.plugin.environment.glite

import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.authentication.GlobusAuthentication
import fr.iscpif.gridscale.information.BDII
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.control.AccessToken
import concurrent.stm._
import java.io.File
import org.openmole.misc.tools.service.{ Random, Hash }
import org.openmole.misc.tools.service.Random._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Scaling._
import scala.annotation.tailrec

trait BDIISRMServers extends BatchEnvironment {
  type SS = GliteStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: GlobusAuthentication.ProxyCreator
  def permissive = false

  @transient lazy val threadsBySE = Workspace.preferenceAsInt(GliteEnvironment.LocalThreadsBySE)

  override def allStorages = {
    val stors = bdiiServer.querySRM(voName, Workspace.preferenceAsDuration(GliteEnvironment.FetchResourcesTimeOut).toSeconds.toInt)
    stors.map {
      s ⇒ GliteStorageService(s, this, proxyCreator, threadsBySE, permissive, GliteAuthentication.CACertificatesDir)
    }
  }

  override def selectAStorage(usedFileHashes: Iterable[(File, Hash)]) =
    if (storages.size == 1) super.selectAStorage(usedFileHashes)
    else {
      val totalFileSize = usedFileHashes.map { case (f, _) ⇒ f.size }.sum
      val onStorage = ReplicaCatalog.withClient(ReplicaCatalog.inCatalog(id)(_))
      val maxTime = storages.map(_.time).max
      val minTime = storages.map(_.time).min

      def fitness =
        storages.flatMap {
          cur ⇒
            cur.tryGetToken match {
              case None ⇒ None
              case Some(token) ⇒
                val sizeOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(h.toString, Set.empty).contains(cur.id) }.map { case (f, _) ⇒ f.size }.sum
                val sizeFactor =
                  if (totalFileSize != 0) (sizeOnStorage.toDouble / totalFileSize) else 0.0

                val time = cur.time
                val timeFactor =
                  if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0) 0.0
                  else 1 - time.normalize(minTime, maxTime)

                val fitness = math.pow((5 * sizeFactor + timeFactor + 10 * cur.availability + 10 * cur.successRate), Workspace.preferenceAsDouble(GliteEnvironment.StorageFitnessPower))
                Some((cur, token, fitness))
            }
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
          val notLoaded = GliteEnvironment.normalizedFitness(fitenesses).shuffled(Random.default)
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
