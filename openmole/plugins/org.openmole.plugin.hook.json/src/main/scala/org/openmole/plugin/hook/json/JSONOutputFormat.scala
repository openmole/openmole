package org.openmole.plugin.hook.json

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.json._

object JSONOutputFormat {

  implicit def outputFormat: OutputFormat[JSONOutputFormat, Any] = new OutputFormat[JSONOutputFormat, Any] {
    override def write(executionContext: HookExecutionContext)(format: JSONOutputFormat, output: WritableOutput, content: OutputContent, method: Any): FromContext[Unit] = FromContext { p ⇒
      import p._
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      implicit val formats = DefaultFormats

      import WritableOutput._
      import OutputFormat._

      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      def sectionContent(sections: SectionContent) =
        JObject(
          sections.sections.map { section ⇒ section.name.from(context) -> variablesToJValue(section.variables) }.toList
        )

      (output, content) match {
        case (Store(file), NamedContent(name, variables)) ⇒
          val f = file / s"${name.from(context)}.json"
          f.from(context).withPrintStream(append = false, create = true) { ps ⇒
            ps.print(compact(render(variablesToJValue(variables))))
          }
        case (Store(file), sections: SectionContent) ⇒
          file.from(context).withPrintStream(append = false, create = true) { ps ⇒
            ps.print(compact(render(sectionContent(sections))))
          }
        case (Display(ps), NamedContent(name, variables)) ⇒
          ps.println(s"${name.from(context)}:")
          ps.println(pretty(render(variablesToJValue(variables))).split("\n").map("  " + _).mkString("\n"))
        case (Display(ps), sections: SectionContent) ⇒
          ps.println(pretty(render(sectionContent(sections))))
      }

    }

    override def validate(format: JSONOutputFormat) = Validate.success
  }

}

case class JSONOutputFormat()
