/**
 * Created by Romain Reuillon on 22/01/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.core.project

import org.openmole.core.compiler.*
import org.openmole.core.pluginmanager.*
import org.openmole.core.script.Imports.{SourceFile, Tree}
import org.openmole.tool.file.*
import monocle.function.all.*
import monocle.std.all.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.fileservice.FileService
import org.openmole.core.project
import org.openmole.core.services.*
import org.openmole.core.workflow.composition.DSL
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.hash.*
import monocle.Focus
import org.openmole.core.script.{Imports, ScriptSourceData}

object Project:

  def newREPL(quiet: Boolean = true)(implicit newFile: TmpDirectory, fileService: FileService) = OpenMOLEREPL.newREPL(quiet = quiet)

  def uniqueName(source: File) = s"_${source.getCanonicalPath.hash()}"

  def scriptsObjects(script: File) = 

    def makeImportTree(tree: Tree): String =
      tree.children.map(c => makePackage(c.name, c.tree)).mkString("\n")

    def makePackage(name: String, tree: Tree): String =
      if (!tree.files.isEmpty) tree.files.distinct.map(f => makeVal(name, f)).mkString("\n")
      else
        s"""
            |class ${name}Clazz {
            |${makeImportTree(tree)}
            |}
            |@transient lazy val $name = new ${name}Clazz""".stripMargin

    def makeVal(identifier: String, file: File) =
      s"""@transient lazy val ${identifier} = ${uniqueName(file)}"""

    def makeScriptWithImports(sourceFile: SourceFile) = 
      def imports = makeImportTree(Tree.insertAll(sourceFile.importedFiles))

      val name = uniqueName(sourceFile.file)

      s"""class ${name}Class {
           |class _importsClazz {
           |$imports
           |}
           |@transient final lazy val _imports = new _importsClazz
           |}
           |@transient lazy val ${name} = new ${name}Class
           """

    def makeImportedScript(sourceFile: SourceFile) = 
      def removeTerms(classContent: String) = 
        import _root_.scala.meta._

        val source = classContent.parse[Source].get
        val cls = source.stats.last.asInstanceOf[Defn.Object]
        val lastStat = cls.templ.stats.last

        def filterTermAndAddLazy(stat: Stat) =
          stat match 
            case _: Term => None
            case v: Defn.Val if v.mods.collect { case x: Mod.Lazy => x }.isEmpty => Some(v.copy(mods = v.mods ++ Seq(Mod.Lazy())))
            case s => Some(s)

        val newCls = cls.copy(templ = cls.templ.copy(stats = cls.templ.stats.flatMap(filterTermAndAddLazy)))
        source.copy(stats = source.stats.dropRight(1) ++ Seq(newCls))

      def imports = makeImportTree(Tree.insertAll(sourceFile.importedFiles))

      def fileString(f: File) = s"""File(new java.net.URI("${f.toURI}").getPath)"""

      val name = uniqueName(sourceFile.file)

      val userDefinitionScopeString =
        val definitionScopeClass = classOf[org.openmole.core.setter.DefinitionScope.UserDefinitionScope].getCanonicalName
        val scope =
          sourceFile.importedBy match
            case Some(value) =>
              val importedDefinitionClass = classOf[org.openmole.core.setter.DefinitionScope.ImportedUserDefinitionScope].getCanonicalName
              s"""$importedDefinitionClass("${value.`import`.fromImport}", ${fileString(value.file)})"""
            case None =>
              s"$definitionScopeClass.default"

        s"""implicit private val _om_definitionLine: ${definitionScopeClass} = $scope"""

      val classContent =
        s"""object ${name} {
           |class _importsClazz {
           |$imports
           |}
           |@transient lazy val _imports = new _importsClazz
           |
           |import _imports._
           |
           |@transient private lazy val ${ConsoleVariables.workDirectory} = ${fileString(sourceFile.file.getParentFileSafe)}
           |$userDefinitionScopeString
           |
           |${sourceFile.file.content}
           |}
        """.stripMargin
      
      removeTerms(classContent)

    val allImports = Imports.importedFiles(script)

    // The first script is the script being compiled itself, no need to include its vars and defs, it would be redundant
    def importHeader = { allImports.take(1).map(makeScriptWithImports) ++ allImports.drop(1).map(makeImportedScript) }.mkString("\n")

    s"""
       |$importHeader
     """.stripMargin

  trait OMSScript:
    def run(variable: ConsoleVariables): DSL

  trait OMSScriptUnit:
    def run(variable: ConsoleVariables): Unit

  def craftedScript(workDirectory: File, script: File, returnUnit: Boolean) = 
    val variableParameter = "_console_variable_parameter"
    def functionPrototype =
      if (returnUnit) s"def run($variableParameter: ${classOf[ConsoleVariables].getCanonicalName}): Unit"
      else s"def run($variableParameter: ${classOf[ConsoleVariables].getCanonicalName}): ${classOf[DSL].getCanonicalName}"

    def traitName =
      if returnUnit
      then s"${classOf[Project.OMSScriptUnit].getCanonicalName}"
      else s"${classOf[Project.OMSScript].getCanonicalName}"

    def scriptHeader =
      val headerContent =
        s"""new $traitName {
           |
           |$functionPrototype = {
           |import $variableParameter._
           |import $variableParameter.services._
           |
           |${scriptsObjects(script)}
           |
           |implicit def _scriptSourceData: ${classOf[ScriptSourceData.ScriptData].getCanonicalName} = ${ScriptSourceData.applySource(workDirectory, script)}
           |import ${Project.uniqueName(script)}._imports._""".stripMargin

      val definitionScopeClass = classOf[org.openmole.core.setter.DefinitionScope.UserDefinitionScope].getCanonicalName
      val userScriptDefinitionScope = classOf[org.openmole.core.setter.DefinitionScope.UserScriptDefinitionScope].getCanonicalName

      s"""$headerContent
         |given $definitionScopeClass = $userScriptDefinitionScope(-${headerContent.split("\n").length + 1})""".stripMargin

    def scriptFooter =
      s"""}
         |}
         """.stripMargin

    def compileContent =
      s"""$scriptHeader
         |${script.content}
         |$scriptFooter""".stripMargin

    (compileContent, scriptHeader)

  def compile(workDirectory: File, script: File, repl: Option[REPL] = None, returnUnit: Boolean = false)(implicit services: Services): CompileResult =
    import services._

    if !script.exists
    then ScriptFileDoesNotExists()
    else 
      def compile(content: String, scriptHeader: String): CompileResult = 
        val loop = repl.getOrElse { Project.newREPL() }
        try 
          Option(loop.compile(content)) match 
            case Some(compiled) => Compiled(compiled, loop, CompilationContext(loop.classDirectory, loop.classLoader), workDirectory = workDirectory, script = script)
            case None           => throw new InternalProcessingError("The compiler returned null instead of a compiled script, it may append if your script contains an unclosed comment block ('/*' without '*/').")
        catch 
          case ce: Interpreter.CompilationError =>
            def positionLens =
              Focus[Interpreter.CompilationError](_.errorMessages) composeTraversal
                each composeLens
                Focus[Interpreter.ErrorMessage](_.position) composePrism
                some

            def headerOffset = scriptHeader.size + 1

            import Interpreter.ErrorPosition

            def adjusted =
              (positionLens composeLens Focus[ErrorPosition](_.line) modify { _ - scriptHeader.split("\n").size + 1 }) andThen
                (positionLens composeLens Focus[ErrorPosition](_ .start) modify { _ - headerOffset }) andThen
                (positionLens composeLens Focus[ErrorPosition](_.end) modify { _ - headerOffset }) andThen
                (positionLens composeLens Focus[ErrorPosition](_.point) modify { _ - headerOffset })

            ErrorInCode(adjusted(ce))
          case e: Throwable => ErrorInCompiler(e)
        

      val (compileContent, scriptHeader) = craftedScript(workDirectory, script, returnUnit = returnUnit)
      compile(compileContent, scriptHeader)

  def completion(workDirectory: File, script: File, position: Int, newREPL: Option[REPL] = None)(implicit services: Services) = 
    import services._
    if(!script.exists()) Vector()
    else
      val (compileContent, scriptHeader) = craftedScript(workDirectory, script, returnUnit = false)
      val loop = newREPL.getOrElse { Project.newREPL() }
      loop.completion(compileContent, position + scriptHeader.size + 1)



sealed trait CompileResult
case class ScriptFileDoesNotExists() extends CompileResult
sealed trait CompilationError extends CompileResult:
  def error: Throwable

case class ErrorInCode(error: Interpreter.CompilationError) extends CompilationError
case class ErrorInCompiler(error: Throwable) extends CompilationError

case class Compiled(result: Interpreter.RawCompiled, repl: REPL, compilationContext: CompilationContext, workDirectory: File, script: File) extends CompileResult:

  def eval(args: Seq[String])(implicit services: Services): DSL =
    import services._

    repl.evalCompiled(result) match
      case p: Project.OMSScript =>
        def consoleVariables = ConsoleVariables(args, workDirectory, experiment = ConsoleVariables.Experiment(ConsoleVariables.experimentName(script)))
        workDirectory.mkdirs()

        p.run(consoleVariables) match 
          case p: DSL => p
          case e => throw new UserBadDataError(s"Script should end with a workflow (it ends with ${if (e == null) null else e.getClass}).")
        
      case e => throw new InternalProcessingError(s"Script should produce an OMScript (found ${if (e == null) null else e.getClass}).")
    


