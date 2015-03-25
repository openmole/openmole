/*
* This file is part of the ToolXiT project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package toolxit.bibtex
package machine

import scala.collection.mutable.{ Map, ListBuffer }
import scala.util.DynamicVariable

/**
 * This trait provides an environment to a machine.
 *
 * @author Lucas Satabin
 *
 */
trait Environment {

  // ==== internal constants ====
  // you may change this values to accept larger strings

  // maximum length of global strings
  protected[this] val globalMax = IntVariable(1000)
  // maximum length of entry strings
  protected[this] val entryMax = IntVariable(1000)

  // ==== the environment ====
  // var name -> value
  protected[this] val globalVariables = Map.empty[String, Variable]
  // var name -> entry name -> value
  protected[this] val entryVariables = Map.empty[String, Map[String, Variable]]
  // field name -> entry name -> value
  protected[this] val fields = Map.empty[String, Map[String, Variable]]
  // name -> instructions
  protected[this] val functions = Map.empty[String, FunctionVariable]
  // name -> value
  protected[this] val macros = Map.empty[String, MacroVariable]
  // value
  protected[this] val preambles = ListBuffer.empty[String]

  /* searches the name in the environment.
   *  - first looks for the name in fields for current entry (if any)
   *  - if not found, looks for the name in entryVariables (if any)
   *  - if not found, looks for the name in globalVariables
   *  - if not found, looks for the name in macros
   *  - if not found, looks for the name in functions */
  protected[this] def lookup(name: String) = {
    var result =
      if (currentEntry.value.isDefined) {
        val entryName = currentEntry.value.get.key
        if (fields.contains(name)) {
          // it is a field
          fields(name).get(entryName)
        }
        else if (entryVariables.contains(name)) {
          // it is not a field, maybe an entry variable
          entryVariables(name).get(entryName)
        }
        else {
          None
        }
      }
      else {
        None
      }

    if (result.isEmpty) {
      // not an entry local variable
      if (globalVariables.contains(name))
        // is it a global variable?
        globalVariables.get(name)
      else if (macros.contains(name))
        // is it a macro?
        macros.get(name)
      else
        // at last lookup for a function
        functions.get(name)
    }
    else {
      result
    }
  }

  /* saves the variable at the appropriate place in the environment
   * if the name is not defined, throws an exception
   * only global and entry variables may be assigned */
  protected[this] def assign(name: String, value: Variable) {
    if (currentEntry.value.isDefined && entryVariables.contains(name)) {
      val entryName = currentEntry.value.get.key
      // truncate to the entry max
      val real = value match {
        case StringVariable(Some(s)) if s.length > entryMax.value.get ⇒
          StringVariable(s.substring(0, entryMax.value.get))
        case _ ⇒ value
      }
      entryVariables(name)(entryName) = real
    }
    else if (globalVariables.contains(name)) {
      // truncate to the global max
      val real = value match {
        case StringVariable(Some(s)) if s.length > globalMax.value.get ⇒
          StringVariable(s.substring(0, globalMax.value.get))
        case _ ⇒ value
      }
      globalVariables(name) = real
    }
    else {
      throw BibTeXException("Unable to run .bst file",
        List(name + " is not declared, thus cannot be assigned"))
    }
  }

  /* only global and entry variables may be assigned */
  protected[this] def canAssign(name: String) =
    (currentEntry.value.isDefined && entryVariables.contains(name)) ||
      globalVariables.contains(name)

  // the entries in the .bib file, sorted first following citation order
  // the SORT command may change the order of bib entries in this list
  // this list is filled when the .bib file is read by the READ command
  protected[this] var entries = List[BibEntry]()

  // contains the currently processed entry
  protected[this] val currentEntry = new DynamicVariable[Option[BibEntry]](None)
  // returns the current entry if any exists
  implicit protected[this] def currentEntryValue = currentEntry.value

  /* resolves the value list to a concatenated string */
  protected[this] def resolve(values: List[Value]) =
    values.foldLeft("") { (res, cur) ⇒
      cur match {
        case StringValue(value) ⇒ res + value
        case NameValue(value) if (macros.contains(value)) ⇒
          res + macros(value)
        case NameValue(value) ⇒ res + value
        case IntValue(value)  ⇒ res + value
        case EmptyValue       ⇒ res
        case _: ConcatValue ⇒ // should not happen!!!
          res
      }
    }

  /* clears all entries in the environment */
  protected[this] def cleanEnv {
    globalVariables.clear
    entryVariables.clear
    fields.clear
    functions.clear
    macros.clear
    entries = Nil
    currentEntry.value = None
  }

  /* initializes the environment with built-in variables and fields */
  protected[this] def initEnv {
    fields("crossref") = Map()
    entryVariables("sort.key$") = Map()
    globalVariables("entry.max$") = entryMax
    globalVariables("global.max$") = globalMax
  }

}

// ==== global variables ====
sealed trait Variable
final case class IntVariable(value: Option[Int] = None) extends Variable
object IntVariable {
  def apply(i: Int): IntVariable = IntVariable(Some(i))
}
final case class StringVariable(value: Option[String] = None) extends Variable
object StringVariable {
  def apply(s: String): StringVariable = StringVariable(Some(s))
}
final case class MacroVariable(value: String) extends Variable
final case class FunctionVariable(instr: bst.BstBlock) extends Variable