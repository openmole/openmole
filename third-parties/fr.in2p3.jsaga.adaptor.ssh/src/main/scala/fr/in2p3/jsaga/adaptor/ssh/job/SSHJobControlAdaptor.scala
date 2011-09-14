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

package fr.in2p3.jsaga.adaptor.ssh.job


import ch.ethz.ssh2.ChannelCondition
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.SFTPv3Client
import ch.ethz.ssh2.Session
import fr.in2p3.jsaga.adaptor.job.control.JobControlAdaptor
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslatorJSDL
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslatorXSLT
import fr.in2p3.jsaga.adaptor.job.control.interactive.StreamableJobInteractiveSet
import fr.in2p3.jsaga.adaptor.ssh.SSHAdaptor
import fr.in2p3.jsaga.adaptor.ssh.data.SFTPDataAdaptor
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.Properties
import java.util.regex.Matcher
import org.ogf.saga.error.NoSuccessException
import scala.util.control.Breaks._
import collection.JavaConversions._
import scala.xml.XML

object SSHJobControlAdaptor {
  
  def exec (connection: Connection, cde: String) = {
    val session = connection.openSession
    try {
      session.execCommand(cde)
      session.waitForCondition(ChannelCondition.EXIT_STATUS, 0)
      if(session.getExitStatus != 0) throw new NoSuccessException("Return code was no 0 but " + session.getExitStatus)
    } finally session.close
  }
  
  /*def waitForCommandToEnd(session: Session, userstdin: InputStream, userstdout: OutputStream, userstderr: OutputStream, commandWait: Int) = session.synchronized {
   val bufferIn = Array.ofDim[Byte](8192)
   val stdin = session.getStdin

   val readIn = new Runnable {
   override def run = while(true){
   val len = userstdin.read(bufferIn, 0, bufferIn.size)
   stdin.write(bufferIn, 0, len)
   }
   }
    
   val thread = new Thread(readIn) {
   setDaemon(true)
   }
    
   thread.run
    
   val bufferOut = Array.ofDim[Byte](8192)
   val stdout = session.getStdout
   val stderr = session.getStderr
    
   breakable {
   while (true) {
   if ((stdout.available == 0) && (stderr.available == 0)) {

   val conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA
   | ChannelCondition.EOF, commandWait)

   if ((conditions & ChannelCondition.TIMEOUT) != 0) 
   throw new IOException("Timeout while waiting for data from peer.")
                

   if ((conditions & ChannelCondition.EOF) != 0) {
   if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) break
   }
   }

   while (stdout.available > 0) {
   val len = stdout.read(bufferOut)
   // this check is somewhat paranoid
   if (len > 0) userstdout.write(bufferOut, 0, len)
   }

   while (stderr.available > 0) {
   val len = stderr.read(bufferOut)
   // this check is somewhat paranoid
   if (len > 0)  userstderr.write(bufferOut, 0, len);
   }
   }
   }
   thread.interrupt
   }*/
  
  //val ROOTDIR = "RootDir"
  
  private class ShellScriptBuffer  {
    var script = ""
    val EOL = "\n"
    
    def +=(s: String) {  script += s + EOL }
    
    override def toString = script
  }
  
  // val workingDirKey = "_WorkingDirectory"
  val executableKey = "_Executable"
}

class SSHJobControlAdaptor extends SSHAdaptor with JobControlAdaptor with CleanableJobAdaptor {

  import SSHJobControlAdaptor._

  override def connect(userInfo: String, host: String, port: Int, basePath: String, attributes: java.util.Map[_, _]) {
    super.connect(userInfo, host, port, basePath, attributes)
    
    val sftpClient = new SFTPv3Client(connection)
    try {
      (SFTPDataAdaptor.getHomeDir(sftpClient) + "/" + SSHJobProcess.rootDir).split("/").tail.foldLeft("") {
        (c, r) => 
        val dir = c + "/" + r
        if(!SFTPDataAdaptor.exists(sftpClient, dir, "")) SFTPDataAdaptor.makeDir(sftpClient, c, r)
        dir
      }
    } finally sftpClient.close
  }
    
  override def getType = "ssh"
	
  override def getDefaultJobMonitor = new SSHJobMonitorAdaptor
  
  override def getJobDescriptionTranslator = new JobDescriptionTranslatorJSDL
  
  //TODO implement stagging
  override def submit(jobDesc: String, checkMath: Boolean, uniqId: String) = {
    val sftpClient = new SFTPv3Client(connection)
 
    try {
      val sjp = new SSHJobProcess(uniqId)
      sjp.setCreated(new Date)
      val command = new ShellScriptBuffer
      
      val x = XML.loadString(jobDesc)

      val application = x \ "JobDescription" \ "Application" \ "POSIXApplication"
      if(!(application \ "WorkingDirectory" isEmpty)) {
        val workDir = (application \ "WorkingDirectory" text) //else absolute(sjp.getJobDir)
        command += "mkdir -p " + workDir
        command += "cd " + workDir
      }

      val executable =  (application \ "Executable" text) + " " + ((application \ "Argument" map(_.text)).foldLeft(""){_ + " " + _})
//Matcher.quoteReplacement(application )
      //println(executable)
 
      val homePath = SFTPDataAdaptor.getHomeDir(sftpClient) 
        
      def absolute(path: String) = homePath + "/" + path
      
      command += "((" +
      executable +
      " > " + absolute(sjp.getOutfile) + " 2> " + absolute(sjp.getErrfile) +" ; " +
      " echo $? > " + absolute(sjp.getEndCodeFile) + ") & " +
      "echo $! > " + absolute(sjp.getPidFile) + " )"

      exec(connection, "bash -c '" + command.toString + "'")
    } catch {
      case e: Exception => throw new NoSuccessException(e)
    } finally sftpClient.close
 
    uniqId
  }

  override def cancel(nativeJobId: String) {
    val cde = "kill `cat " + SSHJobProcess.rootDir + "/" + nativeJobId+".pid`;"
    exec(connection, cde)
  }

  override def clean(nativeJobId: String) {
    val cde = "rm -rf " +  SSHJobProcess.rootDir + "/" + nativeJobId + "*"
    exec(connection, cde)
  }

}
