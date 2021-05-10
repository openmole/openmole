package org.openmole.gui.server.jscompile

import java.io.{ File, InputStream }
import scala.sys.process.{ BasicIO, Process }
import org.openmole.tool.file._

object External {

  def run(name: String, args: Seq[String], workingDir: File): Unit = {
    val cmd = sys.props("os.name").toLowerCase match {
      case os if os.contains("win") ⇒ Seq("cmd", "/c", name)
      case _                        ⇒ Seq(name)
    }

    println("RUNNING " + { cmd ++: args }.mkString(" ") + "in " + workingDir)
    runProcess(cmd ++: args, workingDir)
  }

  private def runProcess[A](cmd: Seq[String], cwd: File): Unit = {
    val toErrorLog = (is: InputStream) ⇒ {
      scala.io.Source.fromInputStream(is).getLines.foreach(msg ⇒ println("ERROR: " + msg))
      is.close()
    }

    //Unfortunately a var is the only way to capture the result
    //    var result: Option[A] = None
    //    def outputCapture(o: InputStream): Unit = {
    //      result = Some(outputProcess(o))
    //      o.close()
    //      ()
    //    }

    println(s"INFO Command: ${cmd.mkString(" ")}")
    val process = Process(cmd, cwd)
    val processIO = BasicIO.standard(false).withError(toErrorLog)
    val code: Int = process.run(processIO).exitValue()

    //    if (code != 0) {
    //      Left(s"Non-zero exit code: $code")
    //    } else {
    //      Right(Some("Rigt result"))
    //    }
  }

  //  def syncLockfile(lockFileName: String,
  //                   baseDir: File,
  //                   installDir: File
  //                  )(command: => Unit): Unit = {
  //    val sourceLockFile = baseDir / lockFileName
  //    val targetLockFile = installDir / lockFileName
  //
  //    if (sourceLockFile.exists()) {
  //      // logger.info("Using lockfile " + sourceLockFile)
  //      sourceLockFile copy targetLockFile
  //    }
  //
  //    command
  //
  //    if (targetLockFile.exists()) {
  //      //  logger.debug("Wrote lockfile to " + sourceLockFile)
  //      targetLockFile copy sourceLockFile
  //    }
  //  }
}