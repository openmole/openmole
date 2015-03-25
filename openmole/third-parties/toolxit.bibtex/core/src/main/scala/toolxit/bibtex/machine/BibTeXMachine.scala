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

import bst._
import scala.util.DynamicVariable
import scala.collection.mutable.{ Map, Stack, ListBuffer, StringBuilder }
import java.io.{ Reader, Writer }
import java.util.logging.{ Logger ⇒ JLogger, Level }

case class BibTeXException(msg: String, errors: List[String]) extends Exception(msg)

/**
 * The BibTeXMachine is a stack machine that accepts a .bst file and
 * a BibTeX file as input and produces the corresponding output.
 *
 * @author Lucas Satabin
 *
 */
class BibTeXMachine(auxReader: Reader,
                    bstReader: Reader,
                    bibReader: Reader,
                    output: Writer)
    extends Environment
    with StackMachine
    with BuiltIn[Unit] {

  lazy val logger = JLogger.getLogger(this.getClass.getName)

  // the output buffer
  private val buffer = new StringBuilder

  // ==== the standard entry types ====
  private val standardTypes = Set(
    "article",
    "book",
    "booklet",
    "conference",
    "inbook",
    "incollection",
    "inproceedings",
    "manual",
    "mastersthesis",
    "misc",
    "phdthesis",
    "proceedings",
    "techreport",
    "unpublished",
    // this one is added to handle unknown entry types
    "default.type")

  val renderingFunction: String ⇒ TOption[BibEntry ⇒ Unit] = {
    case name if functions.contains(name) ⇒
      TSome(entry ⇒ execute(functions(entry.name).instr))
    case _ if functions.contains("default.type") ⇒
      TSome(_ ⇒ execute(functions("default.type").instr))
    case name ⇒
      logger.warning("No `default.type', cannot render entry type: " + name)
      TNone
  }

  lazy val bstfile = try {
    BstParsers.parseAll(BstParsers.bstfile, bstReader) match {
      case BstParsers.Success(parsed, _) ⇒ Some(parsed)
      case error ⇒
        println(error)
        None
    }
  }
  catch {
    case e: Exception ⇒
      e.printStackTrace
      None
  }
  finally {
    bstReader.close
  }

  lazy val auxfile = try {
    val aux = AuxReader.read(auxReader)
    // the BibTeX style used
    val style = aux.find(_.matches("""\\bibstyle\{[^}]+\}"""))
    // list of citations in order they appear in the LaTeX document
    // if a citation appears twice, only the first occurrence is kept
    val citationRegex = """\\citation\{([^}]+)\}"""
    val citations = aux.filter(_.matches(citationRegex)).distinct
      .map(cite ⇒
        citationRegex.r.findFirstMatchIn(cite).get.group(1))
    Some(AuxFile(style, citations))
  }
  catch {
    case e: Exception ⇒
      e.printStackTrace
      None
  }
  finally {
    auxReader.close
  }

  lazy val bibfile = try {
    BibTeXParsers.parseAll(BibTeXParsers.bibFile, bibReader) match {
      case BibTeXParsers.Success(parsed, _) ⇒ Some(parsed)
      case error ⇒
        println(error)
        None
    }
  }
  catch {
    case e: Exception ⇒
      e.printStackTrace
      None
  }
  finally {
    bibReader.close
  }

  /**
   * Runs the machine and produces the output for the input files.
   */
  def run {

    // first make sure that the stack and environment is empty
    stack.clear
    cleanEnv

    // load the automatically-declared variables
    initEnv

    // read the .bst file
    bstfile match {
      case Some(BstFile(commands)) ⇒
        // perform checks on the commands
        check(commands)
        // execute the commands in the file
        execute(commands)
      case _ ⇒ // do nothing
    }

  }

  /* this method checks different rules:
   *  - there is exactly one ENTRY command
   *  - there is exactly one READ command
   *  - ENTRY, MACRO and standard and default FUNCTION commands 
   *    are defined before the READ command
   *  - the READ commands must precede the EXECUTE, ITERATE, 
   *    REVERSE and SORT commands
   */
  private def check(commands: List[BstCommand]) {

    var entrySeen = false
    var readSeen = false
    var standards = Set[String]()
    var errors = List[String]()

    commands.foreach {
      case BstEntry(_, _, _) ⇒
        // only one ENTRY command
        if (entrySeen)
          errors ::= "Duplicated ENTRY command"
        else
          entrySeen = true
        // ENTRY must precede READ
        if (readSeen)
          errors ::= "ENTRY command must precede READ command"
      case BstMacro(_, _) if readSeen ⇒
        // MACRO must precede READ
        errors ::= "MACRO commands must precede READ command"
      case BstFunction(name, _) if standardTypes.contains(name) ⇒
        standards += name
      case BstRead ⇒
        // only one READ command
        if (readSeen)
          errors ::= "Duplicated READ command"
        else
          readSeen = true
        // READ comes after standard types
        val missing = standardTypes &~ standards
        if (missing.nonEmpty)
          errors ::=
            "Standard types must be defined before the READ command. Missing standards types: " + missing.mkString(", ")
      case BstExecute(_) if !readSeen ⇒
        errors ::= "The READ command must precede EXECUTE command"
      case BstIterate(_) if !readSeen ⇒
        errors ::= "The READ command must precede ITERATE command"
      case BstReverse(_) if !readSeen ⇒
        errors ::= "The READ command must precede REVERSE command"
      case BstSort if !readSeen ⇒
        errors ::= "The READ command must precede SORT command"
      case _ ⇒ // all right, just go ahead
    }

    if (errors.nonEmpty)
      throw BibTeXException("Wrong .bst file format", errors)

  }

  /* executes the commands defined in the bst input */
  private def execute(commands: List[BstCommand]): Unit = commands.foreach {
    case BstEntry(fields, integers, strings) ⇒
      fields.foreach { field ⇒
        this.fields(field) = Map()
      }
      integers.foreach { int ⇒
        entryVariables(int) = Map()
      }
      fields.foreach { string ⇒
        entryVariables(string) = Map()
      }
    case BstExecute(fun) ⇒
      functions.get(fun) match {
        case Some(FunctionVariable(instr)) ⇒ execute(instr)
        case None ⇒
          throw BibTeXException("Unable to run .bst file",
            List("FUNCTION " + fun + " is not declared before it is called"))
      }
    case BstFunction(name, instr) ⇒
      functions(name) = FunctionVariable(instr)
    case BstIntegers(names) ⇒
      names.foreach { name ⇒
        globalVariables(name) = IntVariable()
      }
    case BstIterate(fun) ⇒
      functions.get(fun) match {
        case Some(FunctionVariable(instr)) ⇒
          // execute the function for each entry in the entry list
          entries.foreach { entry ⇒
            currentEntry.withValue(Some(entry)) {
              execute(instr)
            }
          }
        case None ⇒
          throw BibTeXException("Unable to run .bst file",
            List("FUNCTION " + fun + " is not declared before it is called"))
      }
    case BstMacro(name, value) ⇒
      macros(name) = MacroVariable(value)
    case BstRead ⇒
      // loads and reads the .bib database
      read
    case BstReverse(fun) ⇒
      functions.get(fun) match {
        case Some(FunctionVariable(instr)) ⇒
          // execute the function for each entry in the entry list in reverse order
          entries.reverse.foreach { entry ⇒
            currentEntry.withValue(Some(entry)) {
              execute(instr)
            }
          }
        case None ⇒
          throw BibTeXException("Unable to run .bst file",
            List("FUNCTION " + fun + " is not declared before it is called"))
      }
    case BstSort ⇒
      // sort the entries
      sort
    case BstStrings(names) ⇒
      names.foreach { name ⇒
        globalVariables(name) = StringVariable()
      }
  }

  /* executes the instructions of a block */
  private def execute(block: BstBlock): Unit =
    block.instructions.foreach {
      case BstPushName(name) ⇒
        push(name)
      case BstRefName(name) ⇒
        // lookup for the name and react accordingly
        lookup(name) match {
          case Some(StringVariable(Some(s))) ⇒
            push(s)
          case Some(StringVariable(_)) ⇒
            push(MissingValue)
          case Some(IntVariable(Some(i))) ⇒
            push(i)
          case Some(IntVariable(_)) ⇒
            push(MissingValue)
          case Some(MacroVariable(m)) ⇒
            push(m)
          case Some(FunctionVariable(code)) ⇒
            // call the function
            execute(code)
          case None ⇒
            throw BibTeXException("Unable to execute .bst file",
              List("Unknown name " + name))
        }
      case BstPushInt(i) ⇒
        push(i)
      case BstPushString(s) ⇒
        push(s)
      case BstSuperior ⇒
        (popInt, popInt) match {
          case (Some(first), Some(second)) ⇒
            if (second > first)
              push(1)
            else
              push(0)
          case pair ⇒
            // error, push 0
            logger.warning("The popped values were: " + pair + ". Unable to compare them.")
            push(0)
        }
      case BstInferior ⇒
        (popInt, popInt) match {
          case (Some(first), Some(second)) ⇒
            if (second < first)
              push(1)
            else
              push(0)
          case pair ⇒
            // error, push 0
            logger.warning("The popped values were: " + pair + ". Unable to compare them.")
            push(0)
        }
      case BstEquals ⇒
        (pop, pop) match {
          case (Some(first), Some(second)) ⇒
            if (second == first)
              push(1)
            else
              push(0)
          case _ ⇒
            // error, push 0
            push(0)
        }
      case BstPlus ⇒
        (popInt, popInt) match {
          case (Some(first), Some(second)) ⇒
            push(first + second)
          case _ ⇒
            // error, push 0
            push(0)
        }
      case BstMinus ⇒
        (popInt, popInt) match {
          case (Some(first), Some(second)) ⇒
            push(second - first)
          case _ ⇒
            // error, push 0
            push(0)
        }
      case BstMultiply ⇒
        (popString, popString) match {
          case (Some(first), Some(second)) ⇒
            push(second + first)
          case _ ⇒
            // error, push null string
            push(NullStringValue)
        }
      case BstAssign ⇒
        (popString, pop) match {
          case (Some(name), Some(value)) if canAssign(name) ⇒
            assign(name, value.toVar)
          case (Some(name), _) ⇒
            throw BibTeXException("Unable to run .bst file",
              List(name + " cannot be assigned"))
          case _ ⇒
            throw BibTeXException("Unable to run .bst file",
              List("Wrong arguments on stack"))
        }
      case block: BstBlock ⇒
        push(FunctionValue(block))
      case BstAddPeriod ⇒
        popString match {
          case Some(s) ⇒
            push(addPeriod$(s))
          case None ⇒
            // error, push null string
            logger.warning("No string on top of the stack, impossible to invoke `add.period$'")
            push(NullStringValue)
        }
      case BstCallType ⇒
        callType$ match {
          case TSome(fun) ⇒
            fun(currentEntry.value.get)
          case TError(msg, _) ⇒
          case TNone          ⇒ // do nothing
        }
      case BstChangeCase ⇒
        (popString, popString) match {
          case (Some("t" | "T"), Some(second)) ⇒
            // to lower case but first letters
            push(toLowerButFirst(second))
          case (Some("l" | "L"), Some(second)) ⇒
            // to lower case
            push(toLower(second))
          case (Some("u" | "U"), Some(second)) ⇒
            // to upper case
            push(toUpper(second))
          case (Some(_), Some(second)) ⇒
            // incorrect pattern, push back the second string
            push(second)
          case _ ⇒
            // error, push null string
            logger.warning("There were not two strings on top of the stack. Unable to change case")
            push(NullStringValue)
        }
      case BstChrToInt ⇒
        popString match {
          case Some(s) if s.length == 1 ⇒
            push(s(0))
          case _ ⇒
            // error, push zero
            push(0)
        }
      case BstCite ⇒
        cite$ match {
          case TSome(key) ⇒
            push(key)
          case TError(msg, _) ⇒
            logger.warning(msg)
            push(NullStringValue)
          case TNone ⇒
            //shall not happen
            throw new RuntimeException("Case should not happen")
        }
      case BstDuplicate ⇒
        pop match {
          case Some(v) ⇒
            push(v)
            push(v)
          case None ⇒
            logger.warning("The stack was empty, nothing to duplicate")
        }
      case BstEmpty ⇒
        pop match {
          case Some(MissingValue | NullStringValue) ⇒
            push(1)
          case Some(SStringValue(s)) if s.matches("\\s*") ⇒
            push(1)
          case _ ⇒ push(0)
        }
      case BstFormatName ⇒
        (popString, popInt, popString) match {
          case (Some(pattern), Some(authorNb), Some(authorList)) ⇒
            formatName$(pattern, authorNb, authorList) match {
              case TSome(s) ⇒ push(s)
              case TError(msg, e) ⇒
                // error, push null string
                logger.warning(msg)
                logger.warning(e.getStackTrace.mkString("\n"))
                push(NullStringValue)
              case TNone ⇒
                //shall not happen
                throw new RuntimeException("Case should not happen")
            }
          case tuple ⇒
            // error, push null string value
            logger.warning("There were not a string, an integer and a string on top of the stack. We found: "
              + tuple)
            push(NullStringValue)
        }
      case BstIf ⇒
        (popFunction, popFunction, popInt) match {
          case (Some(elseFun), Some(thenFun), Some(cond)) ⇒
            if (cond > 0)
              execute(thenFun)
            else
              execute(elseFun)
          case tuple ⇒
            logger.warning("There were two functions and an integer on top of the stack. We found: "
              + tuple)
        }
      case BstIntToChr ⇒
        popInt match {
          case Some(char) ⇒
            push(char.toChar.toString)
          case value ⇒
            // error, push null string
            logger.warning("There was no integer on top of the stack to execute function `int.to.chr$'. We found: " + value)
            push(NullStringValue)
        }
      case BstIntToStr ⇒
        popInt match {
          case Some(char) ⇒
            push(char.toString)
          case value ⇒
            // error, push null string
            logger.warning("There was no integer on top of the stack to execute function `int.to.str$'. We found: " + value)
            push(NullStringValue)
        }
      case BstMissing ⇒
        pop match {
          case Some(MissingValue) ⇒ push(1)
          case _                  ⇒ push(0)
        }
      case BstNewline ⇒
        if (buffer.isEmpty) {
          output.write("\n")
        }
        else {
          // flush the buffer to the file
          output.write(buffer.toString)
          output.flush
          // empty the buffer
          buffer.clear
        }
      case BstNumNames ⇒
        popString match {
          case Some(names) ⇒
            numNames$(names) match {
              case TSome(num) ⇒ push(num)
              case TError(msg, _) ⇒
                logger.warning("Wrongly formatted author list:\n" + msg)
                push(0)
              case TNone ⇒
                //shall not happen
                throw new RuntimeException("Case should not happen")
            }
          case _ ⇒
            // error, push 0
            logger.warning("There was no string on top of the stack")
            push(0)
        }
      case BstPop ⇒
        // pop literal if any
        pop
      case BstPreamble ⇒
        push(preambles.mkString)
      case BstPurify ⇒
        popString match {
          case Some(str) ⇒
            purify$(str) match {
              case TSome(res) ⇒ push(res)
              case TError(msg, _) ⇒
                logger.warning("Failed to execute purify$:\n" + msg)
              case TNone ⇒
                //shall not happen
                throw new RuntimeException("Case should not happen")
            }
          case _ ⇒
            // error, push the null string
            logger.warning("There was no string on top of the stack")
            push(NullStringValue)
        }
      case BstQuote ⇒
        push("\"")
      case BstSkip ⇒
      // no-op
      case BstStack ⇒
        println(stack.mkString("\n")) // TODO improve stack formatting
        stack.clear
      case BstSubstring ⇒
        (popString, popInt, popInt) match {
          case (Some(string), Some(length), Some(start)) ⇒
            // first character is at position one in BibTeX
            push(string.substring(start - 1, length))
          case _ ⇒
            // error, push null string
            push(NullStringValue)
        }
      case BstSwap ⇒
        (pop, pop) match {
          case (Some(first), Some(second)) ⇒
            push(first)
            push(second)
          case (Some(first), None) ⇒
            push(first) // actually, do nothing
          case _ ⇒ // nothing was popped (empty stack), do nothing
        }
      case BstTextLength ⇒
        popString match {
          case Some(str) ⇒
            // TODO, is this really correct according to standard BibTeX behavior?
            import StringUtils.StringParser
            StringParser.parseAll(StringParser.string, str) match {
              case StringParser.Success(res, _) ⇒
                res.foldLeft(0) { (result, current) ⇒
                  result + current.length
                }
              case _ ⇒
                // error, push 0
                push(0)
            }
            push(StringFormatters.flatten(str).length)
          case None ⇒
            // error, push 0
            push(0)
        }
      case BstTextPrefix ⇒
        (popInt, popString) match {
          case (Some(nb), Some(str)) ⇒
            import StringUtils.StringParser
            StringParser.parseAll(StringParser.string, str) match {
              case StringParser.Success(res, _) ⇒
                res.foldLeft(0) { (result, current) ⇒
                  result + current.length
                }
              case _ ⇒
                // error, push 0
                push(0)
            }
          case _ ⇒
            // error, push null string
            push(NullStringValue)
        }
      case BstTop ⇒
        pop.foreach(println _)
      case BstType ⇒
        currentEntry.value match {
          case Some(entry) ⇒
            push(entry.name)
          case None ⇒
            // error, push null string
            push(NullStringValue)
        }
      case BstWarning ⇒
        popString match {
          case Some(str) ⇒
            logger.warning(str)
          case None ⇒ // do nothing
        }
      case BstWhile ⇒
        (popFunction, popFunction) match {
          case (Some(block), Some(cond)) ⇒
            def condition = {
              execute(cond)
              popInt
            }
            while (condition.getOrElse(0) > 0) {
              execute(block)
            }
          case _ ⇒ // do nothing
        }
      case BstWidth ⇒
        popString match {
          case Some(str) ⇒ width$(str) match {
            case TSome(res) ⇒ push(res)
            case TError(msg, err) ⇒
              // error, push 0
              logger.warning(msg)
              logger.warning(err.getStackTrace.mkString("\n"))
              push(0)
            case TNone ⇒
              //shall not happen
              throw new RuntimeException("Case should not happen")
          }
          case _ ⇒
            // error, push 0
            logger.warning("There was no string on top of the stack")
            push(0)
        }
      case BstWrite ⇒
        popString match {
          case Some(str) ⇒
            buffer.append(str)
            // flush the buffer to the file
            output.write(buffer.toString)
            output.flush
            // empty the buffer
            buffer.clear
          case _ ⇒ // do nothing
        }

    }

  /* reads and loads the .bib database */
  private def read {
    (auxfile, bibfile) match {
      case (Some(AuxFile(_, citations)), Some(BibTeXDatabase(entries, strings, pre))) ⇒

        // read all the found entries and enrich the environment with 
        // strings and preambles, and build the map of declared entries
        // in the database
        val bibEntries = Map.empty[String, BibEntry]
        strings.foreach {
          case StringEntry(name, StringValue(value)) ⇒
            macros(name) = MacroVariable(value)
          case StringEntry(name, concat @ ConcatValue(values)) ⇒
            // resolve the values
            val resolved = resolve(values)
            concat.resolved = Some(resolved)
            // set in the environment
            macros(name) = MacroVariable(resolved)
          case _ ⇒ // should never happen, ignore it
        }
        pre.foreach {
          case PreambleEntry(ConcatValue(values)) ⇒
            preambles += resolve(values)
        }
        entries.foreach {
          case b: BibEntry ⇒
            bibEntries(b.key) = b
        }

        def buildEntryList(keys: List[String]): List[BibEntry] = keys match {
          case key :: tail ⇒
            bibEntries.getOrElse(key, UnknownEntry) :: buildEntryList(tail)
          case _ ⇒ List()
        }
        // gets the entries from the database that are in the .aux file
        this.entries = buildEntryList(citations)
      case _ ⇒ // TODO error
    }
  }

  /* sorts the entry list according to the sort.key$ field of the entries */
  private def sort {
    // get the sort key for each entry and set it in the entry
    entries.foreach {
      entry ⇒
        entry.sortKey = entryVariables("sort.key$").get(entry.key) match {
          case Some(StringVariable(Some(k))) ⇒ k
          case _ ⇒ //should never happen if style is correct
            throw BibTeXException("Unable to run .bst file",
              List("All entries should have defined a sort key"))
        }
    }
    // sort the keys according to the sort key
    entries = entries.sortBy(_.sortValue)
  }

}