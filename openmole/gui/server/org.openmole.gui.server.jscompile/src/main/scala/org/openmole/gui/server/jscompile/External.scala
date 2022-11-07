package org.openmole.gui.server.jscompile

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
    val toErrorLog = (is: InputStream) ⇒ {
      is.close()
    }

    //Unfortunately a var is the only way to capture the result
    //    var result: Option[A] = None
    //    def outputCapture(o: InputStream): Unit = {
    //      result = Some(outputProcess(o))
    //      o.close()
    //      ()
    //    }

    val log = new ProcessLogger {
      override def out(s: => String): Unit = {}
      override def err(s: => String): Unit = {
        println(s)
      }
      override def buffer[T](f: => T): T = f
    }


    val process = Process(command = cmd, cwd = Some(cwd), extraEnv = env: _*).!(log)
//    val processIO = BasicIO.standard(_ => ()).withError(_=> ())
//    val code: Int = process.run(processIO).exitValue()

    //    if (code != 0) {
    //      Left(s"Non-zero exit code: $code")
    //    } else {
    //      Right(Some("Rigt result"))
    //    }
  }
}