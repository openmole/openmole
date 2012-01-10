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
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.RelativePath
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.plugin.environment.desktopgrid.DesktopGridJobMessage
import org.openmole.plugin.environment.desktopgrid.SFTPAuthentication
import org.openmole.plugin.environment.desktopgrid.DesktopEnvironment._
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.ReplicatedFile
import org.openmole.core.batch.message.RuntimeResult
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

import org.openmole.core.batch.message.FileMessage._
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.HashMap
import actors.Future
import actors.Futures._

object JobLauncher extends Logger {
  val jobCheckInterval = new ConfigurationLocation("JobLauncher", "jobCheckInterval") 
  Workspace += (jobCheckInterval, "PT1M")
}

class JobLauncher(cacheSize: Long, debug: Boolean) {
  import JobLauncher._
  
  val cache = new FileCache {
    val limit = cacheSize
  }
  
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
    implicit val rng = new Random(id.getLeastSignificantBits ^ id.getMostSignificantBits)
    var runtime: Option[(String, File)] = None

    val storage = "sftp://" + user + '@' + host + ":" + port + "/"
    val relativePath = new RelativePath(storage)
    import relativePath._
        
    def getFileUnzipVerifyHash(fileMessage: FileMessage) = {
      import fileMessage._
      val file =  fileMessage.path.cacheUnziped
      if(file.hash.toString != hash) throw new InternalProcessingError("Wrong hash for file " + path + ".")
      file -> hash
    }

    def getFile(fileMessage: FileMessage) = {
      import fileMessage._
      val file = fileMessage.path.cache
      file -> hash
    }

    while(true) {
      try{
        val resultsDir = resultsDirName.toURIFile
        val jobsDir = jobsDirName.toURIFile
        val timeStempsDir = timeStempsDirName.toURIFile
        
        selectAJob(jobsDir, timeStempsDir, resultsDir, id) match {
          case Some((job, jobMessage))=>
        
            var cached = List.empty[FileMessage]
         
            try {
              val runtime = cache.cache(jobMessage.runtime, 
                                        msg => {
                  val dir = Workspace.newDir
                  logger.info("Downloading the runtime.")
                  val (archive, hash) = getFileUnzipVerifyHash(msg)
                  logger.info("Extracting runtime.")
                  archive.extractUncompressDirArchiveWithRelativePath(dir)
                  dir -> hash
                })
              cached ::= jobMessage.runtime
   
              val pluginDir = Workspace.newDir
            
              jobMessage.runtimePlugins.foreach {
                fileMessage =>
                val plugin = cache.cache(fileMessage, getFileUnzipVerifyHash)
                cached ::= fileMessage
                plugin.copy(File.createTempFile("plugin", ".jar", pluginDir))
              }
              
              val executionMesageFileCache = jobMessage.executionMessagePath.cacheUnziped
              val executionMessage = SerializerService.deserialize[ExecutionMessage](executionMesageFileCache)
              executionMesageFileCache.delete
              
              def localCachedReplicatedFile(replicatedFile: ReplicatedFile) = {
                val localFile = cache.cache(replicatedFile, getFile)
                cached ::= replicatedFile
                import replicatedFile._
                new ReplicatedFile(src, directory, hash, new URIFile(localFile).location, mode)
              }

              val files = executionMessage.files.map(localCachedReplicatedFile) 
              val plugins = executionMessage.plugins.map(localCachedReplicatedFile) 
              val jobs = new FileMessage(new URIFile(executionMessage.jobs.path.cache).location, executionMessage.jobs.hash)
              
              val communicationDirPath = Workspace.newDir
              val localExecutionMessgage = Workspace.newFile("executionMessage", ".gz")
              
              val os = localExecutionMessgage.gzipedBufferedOutputStream
              try SerializerService.serialize(new ExecutionMessage(plugins, files, jobs, new URIFile(communicationDirPath).location), os)
              finally os.close
              
              val localResultFile = Workspace.newFile("job", ".res")
              val configurationDir = Workspace.newDir("configuration")
              val workspaceDir = Workspace.newDir("workspace")
              val osgiDir = new File(runtime, UUID.randomUUID.toString)       
              
              val cmd = "java -Xmx" + jobMessage.memory + "m -Dosgi.configuration.area=" + osgiDir.getName +" -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar -configuration \"" + configurationDir.getAbsolutePath +  "\" -a \"" + authFile.getAbsolutePath + "\" -s \"" + "file://localhost" + "\" -w \"" + workspaceDir.getAbsolutePath  + "\" -i \"" + localExecutionMessgage.getAbsolutePath + "\" -o \"" + new URIFile(localResultFile).location + "\" -c / -p \"" + pluginDir.getAbsolutePath + "\"" + (if(debug) " -d " else "")
            
              logger.info("Executing runtime: " + cmd)
              //val commandLine = CommandLine.parse(cmd)
              val process = Runtime.getRuntime.exec(cmd, null, runtime) //commandLine.toString, null, runtimeLocation)
              executeProcess(process, System.out, System.err)

              configurationDir.recursiveDelete
              workspaceDir.recursiveDelete
              osgiDir.recursiveDelete
              pluginDir.recursiveDelete 
              localExecutionMessgage.delete
                
              val runtimeResult = {
                val is = localResultFile.gzipedBufferedInputStream
                try SerializerService.deserialize[RuntimeResult](is)
                finally is.close
              }
                
              logger.info("Uploading context results")
                              
              def uploadFileMessage(msg: FileMessage) = {
                val localFile = new File(msg.path)
                val uploadedFile = executionMessage.communicationDirPath.toURIFile.newFileInDir("fileMsg", ".bin")
                try URIFile.copy(localFile, uploadedFile)
                finally localFile.delete
                new FileMessage(uploadedFile.path, msg.hash)
              }
                
              val uplodadedResult = runtimeResult.result match {
                case Left(result) => Left(uploadFileMessage(result))
                case Right(e) => Right(e)
              }

              val uploadedStdOut = runtimeResult.stdOut match {
                case Some(stdOut) =>  logger.info("Uploading stdout"); Some(uploadFileMessage(stdOut))
                case None => None
              }
  
              val uploadedStdErr = runtimeResult.stdErr match {
                case Some(stdErr) => logger.info("Uploading stderr"); Some(uploadFileMessage(stdErr))
                case None => None
              }

              logger.info("Context results uploaded" ) 
                
              val resultToSend = new RuntimeResult(
                uploadedStdOut, 
                uploadedStdErr,
                uplodadedResult)
                
              // Upload the result
              val outputLocal = Workspace.newFile("output", ".res")
              logger.info("Uploading job results")
              SerializerService.serialize(resultToSend, outputLocal)
              try URIFile.copy(outputLocal, new GZURIFile(resultsDir.newFileInDir(job, ".res")))
              finally outputLocal.delete
              logger.info("Job results uploaded" )
                
              communicationDirPath.recursiveDelete
              localResultFile.delete
           
              logger.info("Process finished.")
              //if(ret != 0) throw new InternalProcessingError("Error executing: " + commandLine +" return code was not 0 but " + ret)
  
            } finally {
              cache.release(cached)
            }
          case None  => 
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

  
  def selectAJob(jobsDir: IURIFile, timeStempsDir: IURIFile, resultDir: IURIFile, id: UUID)(implicit rng: Random) = {
    val jobs = jobsDir.list
        
    if(!jobs.isEmpty) {
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
      
        possibleChoices._2.map{job => olderTimeStemp(job) -> job}.min(Ordering.by{j: (Long, _) => j._1})._2
            
      }
      logger.info("Choosen job is " + job)
      timeStempsDir.child(job + timeStempSeparator + id.toString).touch
          
      val jobMessage = {
        val os = jobsDir.child(job).cache.gzipedBufferedInputStream
        try SerializerService.deserialize[DesktopGridJobMessage](os)
        finally os.close
      }

      logger.info("Job execution message is " + jobMessage.executionMessagePath)
      Some(job -> jobMessage)
    } else None
  }
  
}
