package org.openmole.plugin.hook.json

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.mole.FromContextHook
import org.openmole.plugin.tool.json._

object JSONOutputFormat {

  implicit def outputFormat = new OutputFormat[JSONOutputFormat] {
    override def write(format: JSONOutputFormat, output: WritableOutput, ps: Seq[Val[_]]): FromContext[Unit] = FromContext { p ⇒
      import p._
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      implicit val formats = DefaultFormats

      val vs = ps.flatMap(v ⇒ context.variable[Any](v.name).toSeq)

      output match {
        case WritableOutput.FileValue(file) ⇒
          file.from(context).withPrintStream(append = false, create = true) { ps ⇒
            ps.print(compact(render(variablesToJValue(vs))))
          }
        case WritableOutput.PrintStreamValue(ps) ⇒ ps.println(pretty(render(variablesToJValue(vs))))
      }
    }

    override def validate(format: JSONOutputFormat): FromContextHook.ValidateParameters ⇒ Seq[Throwable] = { p ⇒ Seq() }
  }

}

case class JSONOutputFormat()
