package org.openmole.plugin.hook.omr

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.CSVOutputFormat
import org.openmole.core.workflow.hook.FromContextHook
import org.openmole.plugin.tool.json._

object OMROutputFormat {

  implicit def outputFormat: OutputFormat[OMROutputFormat, Any] = new OutputFormat[OMROutputFormat, Any] {
    override def write(format: OMROutputFormat, output: WritableOutput, content: OutputContent, method: Any): FromContext[Unit] = FromContext { p ⇒
      import p._
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      implicit val formats = DefaultFormats

      output match {
        case WritableOutput.Display(stream) => implicitly[OutputFormat[CSVOutputFormat, Any]].write(CSVOutputFormat(), output, content, method)
        case WritableOutput.Store(file) =>
          import org.openmole.tool.tar._

//          file.from(context).withTarGZOutputStream {
//
//          }
      }
    }

    override def validate(format: OMROutputFormat): FromContextHook.ValidateParameters ⇒ Seq[Throwable] = { p ⇒ Seq() }
  }

}

case class OMROutputFormat()
