/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external

import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelSftp

abstract class ExternalVirtualTask(name: String) extends ExternalTask(name) {


// def prepareInputFiles(context: IContext, progress: IProgress, tmpDir: File, session: Session) {
//
//
//
//    session.connect( workspace.getPreferenceAsDurationInMs(Configuration.VirtualMachineConnectionTimeOut).intValue )
//
//    try {
//
//        channel = session.openChannel("sftp") match {
//          case ch: ChannelSftp => ch
//          case _ => throw new ClassCastException
//        }
//
//        channel.connect
//
//        channel.cd(remoteDirectory);
//        channel.put(new FileInputStream(localFile), filename);
//        channel.disconnect();
//        session.disconnect();
//
//        listInputFiles(context, progress).foreach( f => {
//        val to = new File(tmpDir, f._2)
//
//        copy(f_1, to)
//
//        applyRecursive(to, new IFileOperation() {
//            override def execute(file: File) =  {
//              if (file.isFile()) {
//                file.setExecutable(true)
//              }
//              file.deleteOnExit
//            }
//          })
//      }
//    )
//    } finally {
//      session.disconnect
//    }
//  }
//
//
//  private def copyTo(local: File, remote: File, channel: ChannelSftp) {
//    channel cd (remote getAbsolutePath)
//    toCopy =
//
//
//  }
//
//
//  def fetchOutputFiles(context: IContext, progress: IProgress, tmpDir: File, session: Session) = {
//    val usedFiles = new TreeSet[File]
//
//    setOutputFilesVariables(context,progress,localDir).foreach( f => {
//        val current = new File(tmpDir,f)
//        if (!current.exists) {
//          throw new UserBadDataError("Output file " + from.getAbsolutePath + " for task " + getName + " doesn't exist")
//        }
//        usedFiles add (current)
//      }
//    )
//
//    val unusedFiles = new ListBuffer[File]
//    val unusedDirs = new ListBuffer[File]
//
//    applyRecursive(tmpDir, new IFileOperation() {
//        override def execute(file: File) =  {
//          if(file.isFile) unusedFiles += (file)
//          else unusedDirs += (file)
//        }
//      }, usedFiles)
//
//    unusedFiles.foreach( f => {
//      f.delete
//    })
//
//    unusedDirs.foreach( d => {
//      if(d.exists && dirContainsNoFileRecursive(d)) recursiveDelete(d)
//    } )
//  }
//

//  protected def execute(cmd: String, session: Session) = {
//      session.connect( workspace.getPreferenceAsDurationInMs(Configuration.VirtualMachineConnectionTimeOut).intValue )
//      try {
//        val channel = session.openChannel("exec") match {
//          case ch: ChannelExec => ch
//          case _ => throw new ClassCastException
//        }
//
//	channel.setCommand(cmd)
//
//        channel.setOutputStream(new PrintStream(System.out)  {
//          override def close() = {}
//        })
//
//        channel.setErrStream(new PrintStream(System.err)  {
//          override def close() = {}
//        })
//
//        // start job
//	channel.connect
//
//        //Ugly active wait
//        while(!channel.isClosed) {
//           Thread.sleep( workspace.getPreferenceAsDurationInMs(Configuration.ActiveWaitInterval).intValue )
//        }
//
//      } finally {
//        session.disconnect
//      }
//  }

}
