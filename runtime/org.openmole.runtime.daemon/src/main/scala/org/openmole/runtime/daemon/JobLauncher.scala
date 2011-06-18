/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.runtime.daemon

import java.net.URI
import java.net.URL
import java.util.Random
import java.util.UUID
import org.openmole.core.batch.file.URIFile
import org.openmole.plugin.environment.desktop.SFTPAuthentication
import org.openmole.plugin.environment.desktop.DesktopEnvironment._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import scala.collection.immutable.TreeMap

object JobLauncher extends Logger {
  val jobCheckInterval = new ConfigurationLocation("JobLauncher", "jobCheckInterval") 
  Workspace += (jobCheckInterval, "PT1M")
}

class JobLauncher {
  import JobLauncher._
  val id = UUID.randomUUID
  val rng = new Random(id.getLeastSignificantBits ^ id.getMostSignificantBits)
  
  def launch(userHostPort: String, password: String) = {
    val splitUser = userHostPort.split("@") 
    if(splitUser.size != 2) throw new UserBadDataError("Host must be formated as user@hostname")
    val user = splitUser(0)
    val splitHost = splitUser(1).split(":")
    val port = if(splitHost.size == 2) splitHost(1).toInt else 22
    val host = splitHost(0)
   
    val auth = new SFTPAuthentication(host, port, user, password)
    auth.initialize
    
    while(true) {
      try{
        val jobs = new URIFile("sftp://" + host + ":" + port + "/" + jobsDirName + '/').list
        
        if(!jobs.isEmpty) {
          val timeStempsDir = new URIFile("sftp://" + host + ":" + port + "/" + timeStempsDirName + '/')
          val timeStemps = timeStempsDir.list
      
          val groupedStemps = timeStemps.map{ts => ts.split(timeStempSeparator).head -> ts}.groupBy{_._1}
          val stempsByJob = jobs.map{j => j -> groupedStemps.getOrElse(j, Iterable.empty).map{_._2}}
          
  
          val possibleChoices = stempsByJob.map{case(j, s) => s.size -> j}.foldLeft(Int.MaxValue -> List.empty[String]){
            (acc, cur) => 
            if(cur._1 < acc._1) cur._1 -> List(cur._2) 
            else if(cur._1 > acc._1) acc
            else acc._1 -> (cur._2 +: acc._2)
          }
          
          logger.info("Choose between " + possibleChoices._2.size + " jobs with " + possibleChoices._1 + " timestemps ")
          
          val job = if(possibleChoices._1 == 0) {
            logger.info("Choose a job at random")
            val index = rng.nextInt(possibleChoices._2.size)
            possibleChoices._2(index)
          } else {
            logger.info("Choose a job with older timestemp")
              
            def olderTimeStemp(job: String) = groupedStemps(job).map{v => timeStempsDir.modificationTime(v._2)}.min
      
            possibleChoices._2.map{job => olderTimeStemp(job) -> job}.min(new Ordering[(Long, String)] {
                override def compare(v1: (Long, String), v2: (Long, String)) = v1._1 compare v2._1
              })._2
            
          }
          logger.info("Job choosen is " + job)
        } else {
          logger.info("Job list is empty on the remote host.")
        }
        Thread.sleep(Workspace.preferenceAsDurationInMs(jobCheckInterval))

      } catch {
        case e: Exception => 
          logger.log(WARNING, "Error while looking for jobs.",e)
          Thread.sleep(Workspace.preferenceAsDurationInMs(jobCheckInterval))
      }
    }
  }
}
