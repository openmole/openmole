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

import java.io.File
import java.net.URI
import java.net.URL
import java.util.Random
import java.util.UUID
import org.apache.commons.exec.CommandLine
import org.openmole.core.batch.file.RelativePath
import org.openmole.core.batch.file.URIFile
import org.openmole.plugin.environment.desktop.DesktopJobMessage
import org.openmole.plugin.environment.desktop.SFTPAuthentication
import org.openmole.plugin.environment.desktop.DesktopEnvironment._
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.ProcessUtil._
import org.openmole.misc.tools.service.ThreadUtil._
import org.openmole.misc.hashservice.HashService._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap

object JobLauncher extends Logger {
  val jobCheckInterval = new ConfigurationLocation("JobLauncher", "jobCheckInterval") 
  Workspace += (jobCheckInterval, "PT1M")
}

class JobLauncher {
  import JobLauncher._

  def launch(userHostPort: String, password: String, nbWorkers: Int) = {
    val splitUser = userHostPort.split("@") 
    if(splitUser.size != 2) throw new UserBadDataError("Host must be formated as user@hostname")
    val user = splitUser(0)
    val splitHost = splitUser(1).split(":")
    val port = if(splitHost.size == 2) splitHost(1).toInt else 22
    val host = splitHost(0)
   
    val auth = new SFTPAuthentication(host, port, user, password)
    auth.initialize
    
    val authFile = Workspace.newFile("auth", ".xml")
    SerializerService.serialize(auth, authFile)
    
    (0 until nbWorkers).foreach{
      i => 
        background{runJobs(user, host, port, password, authFile)}
    }
    Thread.currentThread.join
  }
  
  def runJobs(user: String, host: String, port: Int, password: String, authFile: File) = {
    val id = UUID.randomUUID
    val rng = new Random(id.getLeastSignificantBits ^ id.getMostSignificantBits)
    var runtime: Option[(String, File)] = None
    
 
    
    val storage = "sftp://" + host + ":" + port + "/"
    val relativePath = new RelativePath(storage)
    
    import relativePath._
    
    def getFileVerifyHash(fileMessage: FileMessage) = {
      import fileMessage._
      val file = path.cacheUnziped
      if(file.hash.toString != hash) throw new InternalProcessingError("Wrong hash for file " + path + ".")
      file
    }
    
    while(true) {
      try{
        val jobsDir = jobsDirName.toURIFile
        val jobs = jobsDir.list
        
        if(!jobs.isEmpty) {
          val timeStempsDir = timeStempsDirName.toURIFile
          val timeStemps = timeStempsDir.list
          
          val resultsDir = resultsDirName.toURIFile

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
          logger.info("Choosen job is " + job)
          timeStempsDir.child(job + timeStempSeparator + id.toString).touch
          
          val os = jobsDir.child(job).cache.gzipedBufferedInputStream
          val jobMessage = 
            try SerializerService.deserialize[DesktopJobMessage](os)
          finally os.close
          
          logger.info("Job execution message is " + jobMessage.executionMessagePath)
          
          runtime.foreach{
            case(hash, file) => 
              if(hash != jobMessage.runtime.hash) {
                logger.info("Deleting outdated runtime.")
                file.recursiveDelete
                runtime = None
              } 
          }
            
          val runtimeLocation = runtime match {
            case None => 
              val dir = Workspace.newDir
              logger.info("Downloading the runtime.")
              val runtimeArchive = getFileVerifyHash(jobMessage.runtime) 
              logger.info("Extracting runtime.")
              runtimeArchive.extractDirArchiveWithRelativePath(dir)
              runtime = Some(jobMessage.runtime.hash -> dir)
              dir
            case Some(r) => r._2
          }
          
          logger.info("Downloading environment plugins.")
          val pluginDir = Workspace.newDir
          try {
            jobMessage.runtimePlugins.foreach {
              fileMessage => 
              val file = getFileVerifyHash(fileMessage)
              file.renameTo(File.createTempFile("plugin", ".jar", pluginDir))
            }
            

            val resultFile = resultsDir.newFileInDir(job, ".res")
            val configurationDir = Workspace.newDir
            val workspaceDir = Workspace.newDir
            try {
              
              val cmd = "java -Xmx" + jobMessage.memory +"m -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar -configuration " + configurationDir.getAbsolutePath +  " -a " + authFile.getAbsolutePath + " -s " + storage + " -w " + workspaceDir.getAbsolutePath  + " -i " + jobMessage.executionMessagePath + " -o " + resultFile.URI.toString + " -c / -p " + pluginDir.getAbsolutePath
            
              logger.info("Executing runtime: " + cmd + ".")
              //val commandLine = CommandLine.parse(cmd)
              val process = Runtime.getRuntime.exec(cmd, null, runtimeLocation) //commandLine.toString, null, runtimeLocation)
              executeProcess(process, System.out, System.err)
            } finally {
              configurationDir.recursiveDelete
              workspaceDir.recursiveDelete
            }
           
            logger.info("Process finished.")
            //if(ret != 0) throw new InternalProcessingError("Error executing: " + commandLine +" return code was not 0 but " + ret)
           
          } finally pluginDir.recursiveDelete 
        } else {
          logger.info("Job list is empty on the remote host.")
          Thread.sleep(Workspace.preferenceAsDurationInMs(jobCheckInterval))
        }
 
      } catch {
        case e: Exception => 
          logger.log(WARNING, "Error while looking for jobs.",e)
          Thread.sleep(Workspace.preferenceAsDurationInMs(jobCheckInterval))
      }
    }
  }
  

}
