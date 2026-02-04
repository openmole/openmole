package org.openmole.core.compiler

import org.osgi.framework.Bundle
import org.openmole.tool.file.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.fileservice.FileService
import cats.instances.int
import org.openmole.core.exception.InternalProcessingError

object REPL:
  def apply(priorityBundles: => Seq[Bundle] = Nil, jars: Seq[File] = Seq.empty, quiet: Boolean = true, options: Seq[String] = Seq())(using TmpDirectory, FileService) =
    val interpreter = Interpreter(priorityBundles, jars, quiet, options)
    new REPL(interpreter)

class REPL(interpreter: Interpreter):
  export interpreter.classDirectory

  var state = interpreter.initialState

  def eval(code: String) =
    val compiled = compile(code)
    evalCompiled(compiled)

  def evalCompiled(compiled: Interpreter.RawCompiled) = synchronized:
    val (result, s1) = interpreter.run(compiled)
    //println("res " + result.getClass.getDeclaredMethods.toSeq)
    state = s1
    result

  def compile(code: String): Interpreter.RawCompiled = interpreter.dottyCompile(code, state)
  def completion(code: String, position: Int): Vector[Interpreter.CompletionCandidate] = synchronized(interpreter.completion(code, position, state))

  def bind[T: Manifest](name: String, value: T) = synchronized:
    val mutableName = s"__${name}_mutable_value"

    def findClass = 
      val index = (1 to state.objectIndex).findLast(i => interpreter.resultClass(state, Some(i)).getDeclaredMethods.exists(_.getName.contains(s"${mutableName}_$$eq")))
      index.map(i => interpreter.resultClass(state, Some(i)))

//    eval(s"""var $mutableName: ${manifest[T].toString} = null""")
//    val mutableClass = findClass.getOrElse(throw new InternalProcessingError("Mutable value class not found"))

    val mutableClass =
      findClass match
        case Some(cl) => cl
        case None =>
          eval(s"""var $mutableName: ${manifest[T].toString} = null""")
          findClass.getOrElse(throw new InternalProcessingError("Mutable value class not found"))

    // FIXME add assertion about type of mutable value

    val eqMethod = mutableClass.getDeclaredMethods.find(_.getName.contains(s"${mutableName}_$$eq")).get
    //mutableField.setAccessible(true)
    eqMethod.invoke(null, value)
    eval(s"val $name = $mutableName: ${manifest[T].toString}")

  def loop(terminal: Option[org.jline.terminal.Terminal]) = synchronized:
    state = interpreter.driver.runUntilQuit(using state)(terminal)

  def classLoader = synchronized:
    interpreter.classLoader(state.context)

  def close = interpreter.close

