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

    runProcess(cmd ++: args, workingDir)
  }

  private def runProcess[A](cmd: Seq[String], cwd: File): Unit = {
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

    val process = Process(cmd, cwd)
    val processIO = BasicIO.standard(_ => ()).withError(_=> ())
    val code: Int = process.run(processIO).exitValue()

    //    if (code != 0) {
    //      Left(s"Non-zero exit code: $code")
    //    } else {
    //      Right(Some("Rigt result"))
    //    }
  }
}