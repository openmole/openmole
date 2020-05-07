//package org.openmole.plugin.hook.omr
//
//import org.openmole.core.dsl._
//import org.openmole.core.dsl.extension._
//import org.openmole.core.workflow.hook.FromContextHook
//import org.openmole.plugin.tool.json._
//
//object OMROutputFormat {
//
//  implicit def outputFormat = new OutputFormat[OMROutputFormat, Any] {
//    override def write(format: OMROutputFormat, output: WritableOutput, variables: Seq[Variable[_]]): FromContext[Unit] = FromContext { p ⇒
//      import p._
//      import org.json4s._
//      import org.json4s.jackson.JsonMethods._
//
//      implicit val formats = DefaultFormats
//
//      output match {
//        case WritableOutput.FileValue(file) ⇒
//          file.from(context).withPrintStream(append = false, create = true) { ps ⇒
//            ps.print(compact(render(variablesToJValue(variables))))
//          }
//        case WritableOutput.StreamValue(ps, prelude) ⇒
//          prelude.foreach(ps.print)
//          ps.println(pretty(render(variablesToJValue(variables))))
//      }
//    }
//
//    override def validate(format: OMROutputFormat): FromContextHook.ValidateParameters ⇒ Seq[Throwable] = { p ⇒ Seq() }
//    override def extension = ".omr"
//  }
//
//}
//
//case class OMROutputFormat()
