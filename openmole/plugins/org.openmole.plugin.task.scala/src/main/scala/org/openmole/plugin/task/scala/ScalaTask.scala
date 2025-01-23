/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.scala

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.plugin.task.external.*

import monocle.*
import _root_.scala.util.*

object ScalaTask:
  given InputOutputBuilder[ScalaTask] = InputOutputBuilder(Focus[ScalaTask](_.config))
  given ExternalBuilder[ScalaTask] = ExternalBuilder(Focus[ScalaTask](_.external))
  given InfoBuilder[ScalaTask] = InfoBuilder(Focus[ScalaTask](_.info))
  given MappedInputOutputBuilder[ScalaTask] = MappedInputOutputBuilder(Focus[ScalaTask](_.mapped))

  given JVMLanguageBuilder[ScalaTask] = new JVMLanguageBuilder[ScalaTask]:
    override def libraries = Focus[ScalaTask](_.libraries)
    override def plugins = Focus[ScalaTask](_.userPlugins)

  def defaultPlugins = pluginsOf(scala.xml.XML).toVector

  def apply(source: String, libraries: Seq[File] = Vector(), plugins: Seq[File] = Vector())(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new ScalaTask(
      source,
      userPlugins = defaultPlugins ++ plugins,
      libraries = libraries.toVector,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig()
    )

  def apply(f: (Context, ⇒ _root_.scala.util.Random) ⇒ Seq[Variable[?]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("ScalaTask")(p => Context(f(p.context, p.random())*))

  def apply(f: Context ⇒ Seq[Variable[?]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("ScalaTask")(p => Context(f(p.context)*))


case class ScalaTask(
  sourceCode:  String,
  userPlugins: Vector[File],
  libraries:   Vector[File],
  config:      InputOutputConfig,
  external:    External,
  info:        InfoConfig,
  mapped:      MappedInputOutputConfig
) extends Task with ValidateTask:

  lazy val compilation = CacheKey[ScalaCompilation.ContextClosure[java.util.Map[String, Any]]]()
  lazy val pluginsCache = CacheKey[Seq[File]]()

  private def toMappedInputVals(ps: PrototypeSet, mapped: Seq[ Mapped[?]]) =
    ps /*-- mapped.map(_.v)*/ ++ mapped.map(m => m.v.withName(m.name))

  private def toMappedOutputVals(ps: PrototypeSet, mapped: Seq[ Mapped[?]]) =
    ps -- mapped.map(_.v) ++ mapped.map(m => m.v.withName(m.name))

  lazy val noFileInputs = Mapped.noFile(mapped.inputs)
  lazy val noFileOutputs = Mapped.noFile(mapped.outputs)
  lazy val mappedInputs = toMappedInputVals(Task.inputs(this), noFileInputs)
  lazy val mappedOutputs = toMappedOutputVals(Task.outputs(this), noFileOutputs)

  lazy val externalWithFiles =
    external.copy(
      inputFiles = external.inputFiles ++ Mapped.files(mapped.inputs).map { m => External.InputFile(m.v, m.name, true) },
      outputFiles = external.outputFiles ++ Mapped.files(mapped.outputs).map { m => External.OutputFile(m.name, m.v) }
    )

  def plugins(using tmpDirectory: TmpDirectory, fileService: FileService, cache: KeyValueCache): Seq[File] =
    val detectedPlugins =
      cache.getOrElseUpdate(pluginsCache):
        import org.openmole.tool.bytecode.*
        val compiled = cache.getOrElseUpdate(compilation)(compile(mappedInputs.toSeq))
        val mentionedClasses = allMentionedClasses(compiled.interpreter.classDirectory, compiled.interpreter.classLoaderValue)
        mentionedClasses.flatMap(PluginManager.pluginsForClass).distinct

    (userPlugins ++ detectedPlugins).distinct

  def compile(inputs: Seq[Val[?]])(implicit newFile: TmpDirectory, fileService: FileService) =
  //implicit def m: Manifest[java.util.Map[String, Any]] = manifest[java.util.Map[String, Any]]
    ScalaCompilation.static(
      sourceCode,
      mappedInputs.toSeq ++ Seq(JVMLanguageTask.workDirectory),
      ScalaCompilation.WrappedOutput(mappedOutputs),
      libraries = libraries,
      plugins = userPlugins
    )

  override def validate =
    def libraryErrors: Seq[Throwable] = libraries.flatMap: l ⇒
      if !l.exists()
      then Some(new UserBadDataError(s"Library file $l does not exist"))
      else None

    def pluginsErrors: Seq[Throwable] = userPlugins.flatMap: l ⇒
      if !l.exists()
      then Some(new UserBadDataError(s"Plugin file $l does not exist"))
      else None

    Validate: p ⇒
      import p._

      def compilationError =
        Try(cache.getOrElseUpdate(compilation)(compile(mappedInputs.toSeq))) match
          case Success(_) ⇒ Seq.empty
          case Failure(e) ⇒ Seq(e)

      libraryErrors ++ pluginsErrors ++ compilationError


  override def apply(taskBuildContext:  TaskExecutionBuildContext) =
    val pluginsValue =
      import taskBuildContext.given
      given KeyValueCache = taskBuildContext.cache
      plugins

    TaskExecution.withPlugins(pluginsValue): p =>
      import p.*

      def toMappedInputContext(context: Context, mapped: Seq[ Mapped[?]]) =
        context /*-- mapped.map(_.v.name)*/ ++ mapped.map(m => context.variable(m.v).get.copy(prototype = m.v.withName(m.name)))

      def toMappedOutputContext(context: Context, mapped: Seq[ Mapped[?]]) =
        context -- mapped.map(_.v.name) ++ mapped.map(m => context.variable(m.v.withName(m.name)).get.copy(prototype = m.v))

      def processCode =
        FromContext: p ⇒
          import p._

          val scalaCompilation =
            executionContext.cache.getOrElseUpdate(compilation)(compile(mappedInputs.toSeq))

          val map = scalaCompilation(context, p.random, p.tmpDirectory)
          mappedOutputs.toSeq.map {
            o ⇒ Variable.unsecure(o, Option(map.get(o.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
          }: Context

      def mappedContext = toMappedInputContext(context, noFileInputs)
      def resultContext = JVMLanguageTask.process(executionContext, libraries, externalWithFiles, processCode, mappedOutputs).from(mappedContext)
      toMappedOutputContext(resultContext, noFileOutputs)
