package org.openmole.core.format

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.JArray
import org.openmole.core.context.Variable
import org.openmole.core.script.Imports.*
import org.openmole.core.format.*
import org.openmole.core.script.*
import org.openmole.core.json.*
import org.openmole.core.exception.*
import org.openmole.core.argument.*
import org.openmole.core.format.OutputFormat.*
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}

import java.io.{PrintStream, SequenceInputStream}

case class OMROption(script: Boolean = true, overwrite: Boolean = true, append: Boolean = false)

object OMROutputFormat:

  def write[M](
    executionContext: FormatExecutionContext,
    output: WritableOutput,
    content: OutputContent,
    method: M,
    option: OMROption = OMROption())(using methodData: MethodMetaData[M]): FromContext[Unit] =
    FromContext: p =>
      import p.*
      output match
        case WritableOutput.Display(ps) ⇒
          def writeStream(ps: java.io.PrintStream, section: Option[String], variables: Seq[Variable[_]]) =
            def headerLine(variables: Seq[Variable[_]]) = CSVFormat.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = false)

            section match
              case None ⇒
                val header = Some(headerLine(variables))
                CSVFormat.appendVariablesToCSV(ps, header, variables.map(_.value))
              case Some(section) ⇒
                ps.println(section + ":")
                val header = Some(headerLine(variables))
                CSVFormat.appendVariablesToCSV(ps, header, variables.map(_.value), margin = "  ")

          for {(section, i) ← content.section.zipWithIndex}
            val sectionName = if content.section.size > 1 then Some(section.name.getOrElse(s"$i")) else None
            writeStream(ps, sectionName, section.variables)
        case WritableOutput.Store(file) ⇒
          OMROutputFormat.write(executionContext, file.from(p.context), content, method, append = option.append, option = option).from(context)


  def write[M](
    executionContext: FormatExecutionContext,
    omrFile: File,
    content: OutputContent,
    method: M,
    append: Boolean,
    option: OMROption)(using methodData: MethodMetaData[M], scriptData: ScriptSourceData): FromContext[Unit] =
    FromContext: p =>
      import p.*
      import org.json4s.*
      import executionContext.serializerService
      import executionContext.tmpDirectory
      import executionContext.timeService

      given Encoder[M] = methodData.encoder

      def executionId = executionContext.moleExecutionId

      def methodFile =
        omrFile match
          case f if f.getName.endsWith(".omr") => f
          case f => f.getParentFile / s"${f.getName}.omr"

      def methodJson =
        method.asJson.mapObject(_.add(methodNameField, Json.fromString(methodData.name)))

      def script =
        scriptData match
          case data: ScriptSourceData.ScriptData if option.script ⇒
            val scriptContent = ScriptSourceData.scriptContent(scriptData)
            val imports =
              val is = Imports.directImportedFiles(data.script).map(i ⇒ OMRContent.Import(ImportedFile.identifier(i), i.file.content))
              if is.isEmpty then None else Some(is)

            Some(OMRContent.Script(scriptContent, imports))
          case _ ⇒ None


      OMRFormat.write(
        data = content,
        methodFile = methodFile,
        executionId = executionId,
        jobId = executionContext.jobId,
        methodJson = methodJson,
        script = script,
        timeStart = executionContext.moleLaunchTime,
        openMOLEVersion = org.openmole.core.buildinfo.version.value,
        append = append,
        overwrite = option.overwrite
      )


