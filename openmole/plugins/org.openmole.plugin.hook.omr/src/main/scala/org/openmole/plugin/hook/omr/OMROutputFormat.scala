package org.openmole.plugin.hook.omr

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workflow.format.CSVOutputFormat
import org.openmole.core.workflow.hook.FromContextHook
import org.openmole.plugin.tool.json._
import io.circe._
import org.openmole.core.workflow.format.OutputFormat.{ PlainContent, SectionContent }
import org.openmole.core.workflow.format.WritableOutput.Store
import org.openmole.plugin.hook.json.JSONOutputFormat

object OMROutputFormat {

  def methodFile = "method.omr"
  def dataDirectory = "data"

  implicit def outputFormat[MD](implicit encoder: Encoder[MD], methodData: MethodData[MD]): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD] {
    override def write(executionContext: HookExecutionContext)(format: OMROutputFormat, output: WritableOutput, content: OutputContent, method: MD): FromContext[Unit] = FromContext { p ⇒
      import p._
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      output match {
        case WritableOutput.Display(_) ⇒ implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          val directory = file.from(context)

          def methodFormat(json: Json) = {
            json.deepDropNullValues.mapObject(_.add("method", Json.fromString(methodData.name(method))))
          }

          directory.withLockInDirectory {
            import io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

            content match {
              case PlainContent(variables, name) ⇒
                val f =
                  name match {
                    case Some(n) ⇒ directory / dataDirectory / s"${n.from(context)}.json.gz"
                    case None    ⇒ directory / dataDirectory / "data.json.gz"
                  }

                f.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(variablesToJValue(variables))))
                }

                val m = directory / methodFile

                m.withPrintStream(create = true, gz = true)(_.print(methodFormat(method.asJson).noSpaces))
              case sections: SectionContent ⇒
                def sectionContent(sections: SectionContent) =
                  JObject(
                    sections.sections.map { section ⇒ section.name.from(context) -> variablesToJValue(section.variables) }.toList
                  )

                val f = directory / dataDirectory / "data.json.gz"

                f.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(sectionContent(sections))))
                }

                val m = directory / methodFile
                m.withPrintStream(create = true, gz = true)(_.print(methodFormat(method.asJson).noSpaces))
            }
          }
      }
    }

    override def validate(format: OMROutputFormat): FromContextHook.ValidateParameters ⇒ Seq[Throwable] = { p ⇒ Seq() }
  }

}

case class OMROutputFormat()
