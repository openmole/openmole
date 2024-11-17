package org.openmole.gui.server.jscompile

import org.openmole.core.exception.InternalProcessingError

import java.io.{File, InputStream}
import scala.sys.process.{BasicIO, Process, ProcessLogger}
import org.openmole.tool.file.*

object External {

  def run(name: String, args: Seq[String], workingDir: File, env: Seq[(String, String)] = Seq()): Unit = {
    val cmd = sys.props("os.name").toLowerCase match {
      case os if os.contains("win") ⇒ Seq("cmd", "/c", name)
      case _                        ⇒ Seq(name)
    }

    runProcess(cmd ++: args, workingDir, env)
  }

  private def runProcess[A](cmd: Seq[String], cwd: File, env: Seq[(String, String)] = Seq()): Unit = {
    //Unfortunately a var is the only way to capture the result
    //    var result: Option[A] = None
    //    def outputCapture(o: InputStream): Unit = {
    //      result = Some(outputProcess(o))
    //      o.close()
    //      ()
    //    }

    val outputBuffer = new StringBuffer()

    val log = new ProcessLogger {
      override def out(s: => String): Unit = outputBuffer.append(s + "\n")
      override def err(s: => String): Unit = outputBuffer.append(s + "\n")
      override def buffer[T](f: => T): T = f
    }

    Process(command = cmd, cwd = Some(cwd), extraEnv = env *).!(log) match
      case 0 =>
      case x =>
        throw new InternalProcessingError(
          s"""Return code of command ${cmd.mkString(" ")} as returned a non zero value: $x
             |Output was:
             |${outputBuffer.toString}""".stripMargin)

//    val processIO = BasicIO.standard(_ => ()).withError(_=> ())
//    val code: Int = process.run(processIO).exitValue()

    //    if (code != 0) {
    //      Left(s"Non-zero exit code: $code")
    //    } else {
    //      Right(Some("Rigt result"))
    //    }
  }
}