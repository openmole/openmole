package org.openmole.core.compiler

import org.osgi.framework.Bundle
import org.openmole.tool.file._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.fileservice.FileService
import cats.instances.int

object REPL {
  
  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[File] = Seq.empty, quiet: Boolean = true)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val interpreter = Interpreter(priorityBundles, jars, quiet)
    new REPL(interpreter)
  }

}


class REPL(interpreter: Interpreter) {
  export interpreter.classDirectory

  var state = interpreter.initialState

  def eval(code: String) =
    val compiled = compile(code)
    evalCompiled(compiled)

  def evalCompiled(compiled: repl.ReplDriver.Compiled) = synchronized {
    val (result, s1) = interpreter.run(compiled)
    state = s1
    result
  }

  def compile(code: String): Interpreter.RawCompiled = interpreter.dottyCompile(code, state)

  def bind[T: Manifest](name: String, value: T) = synchronized {
    val mutableName = s"__${name}_mutable_value"
    eval(s"""var $mutableName: ${manifest[T].toString} = null""")
    val lastClazz = interpreter.resultClass(state)
    val eqMethod = lastClazz.getDeclaredMethods.find(_.getName.contains(s"${mutableName}_$$eq")).get
    //mutableField.setAccessible(true)
    eqMethod.invoke(null, value)
    eval(s"val $name = $mutableName")
  }

  def loop = synchronized {
    state = interpreter.driver.runUntilQuit(state)
  }

  def classLoader = synchronized { interpreter.classLoader(state.context) }

  def close = interpreter.close

}