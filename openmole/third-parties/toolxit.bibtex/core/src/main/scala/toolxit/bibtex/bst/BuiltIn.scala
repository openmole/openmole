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
package bst

/**
 * This trait offers a collection of built-in bst functions translated into Scala.
 *
 * @author Lucas Satabin
 *
 */
trait BuiltIn[Rendered] {

  import macros.OctalLiterals._

  private val formatters = scala.collection.mutable.Map.empty[String, NameFormatter]

  // character width
  private lazy val widths = {
    val res = Array.fill[Int](o"200")(0)
    res(o"40") = 278
    res(o"41") = 278
    res(o"42") = 500
    res(o"43") = 833
    res(o"44") = 500
    res(o"45") = 833
    res(o"46") = 778
    res(o"47") = 278
    res(o"50") = 389
    res(o"51") = 389
    res(o"52") = 500
    res(o"53") = 778
    res(o"54") = 278
    res(o"55") = 333
    res(o"56") = 278
    res(o"57") = 500
    res(o"60") = 500
    res(o"61") = 500
    res(o"62") = 500
    res(o"63") = 500
    res(o"64") = 500
    res(o"65") = 500
    res(o"66") = 500
    res(o"67") = 500
    res(o"70") = 500
    res(o"71") = 500
    res(o"72") = 278
    res(o"73") = 278
    res(o"74") = 278
    res(o"75") = 778
    res(o"76") = 472
    res(o"77") = 472
    res(o"100") = 778
    res(o"101") = 750
    res(o"102") = 708
    res(o"103") = 722
    res(o"104") = 764
    res(o"105") = 681
    res(o"106") = 653
    res(o"107") = 785
    res(o"110") = 750
    res(o"111") = 361
    res(o"112") = 514
    res(o"113") = 778
    res(o"114") = 625
    res(o"115") = 917
    res(o"116") = 750
    res(o"117") = 778
    res(o"120") = 681
    res(o"121") = 778
    res(o"122") = 736
    res(o"123") = 556
    res(o"124") = 722
    res(o"125") = 750
    res(o"126") = 750
    res(o"127") = 1028
    res(o"130") = 750
    res(o"131") = 750
    res(o"132") = 611
    res(o"133") = 278
    res(o"134") = 500
    res(o"135") = 278
    res(o"136") = 500
    res(o"137") = 278
    res(o"140") = 278
    res(o"141") = 500
    res(o"142") = 556
    res(o"143") = 444
    res(o"144") = 556
    res(o"145") = 444
    res(o"146") = 306
    res(o"147") = 500
    res(o"150") = 556
    res(o"151") = 278
    res(o"152") = 306
    res(o"153") = 528
    res(o"154") = 278
    res(o"155") = 833
    res(o"156") = 556
    res(o"157") = 500
    res(o"160") = 556
    res(o"161") = 528
    res(o"162") = 392
    res(o"163") = 394
    res(o"164") = 389
    res(o"165") = 556
    res(o"166") = 528
    res(o"167") = 722
    res(o"170") = 528
    res(o"171") = 528
    res(o"172") = 444
    res(o"173") = 500
    res(o"174") = 1000
    res(o"175") = 500
    res(o"176") = 500
    res
  }

  /**
   * a function that, given an entry name, returns the rendering function
   * if any. Returns `None' if none found.
   */
  val renderingFunction: String ⇒ TOption[BibEntry ⇒ Rendered]

  /**
   * Adds a ‘.’ to it if the last non‘}’ character isn’t a ‘.’, ‘?’, or ‘!’,
   * and returns this resulting string.
   */
  def addPeriod$(string: String) = {
    val periods = List('.', '!', '?')
    val lastNonBraceIndex = string.lastIndexWhere(_ != '}')
    if (lastNonBraceIndex >= 0 && periods.contains(string(lastNonBraceIndex)))
      string
    else
      string + "."
  }

  /**
   * Executes the function whose name is the entry type of an en-
   * try. For example if an entry is of type book, this function executes the
   * book function. When given as an argument to the ITERATE command,
   * call.type$ actually produces the output for the entries. For an entry
   * with an unknown type, it executes the function default.type. Thus you
   * should define (before the READ command) one function for each standard
   * entry type as well as a default.type function.
   *
   * In this case, it calls the `render*' method
   */
  def callType$(implicit entry: Option[BibEntry]): TOption[BibEntry ⇒ Rendered] =
    entry match {
      case Some(e) ⇒
        renderingFunction(e.name)
      case None ⇒
        TError("There is no current entry, unable to execute the `call.type$' function")
    }

  /**
   * Turns a string to lower case, except for the first letter and for
   * letters that appear at brace-depth strictly positive. Remember
   * that special characters are at depth 0
   */
  def toLowerButFirst(s: String) =
    StringFormatters.toLowerButFirst(s)

  /** Turns S to lower case, except parts that are at strictly positive brace-depth */
  def toLower(s: String) =
    StringFormatters.toLower(s)

  /** Turns S to upper case, except parts that are at strictly positive brace-depth */
  def toUpper(s: String) =
    StringFormatters.toUpper(s)

  /**
   * Returns the internal key of the current entry.
   */
  def cite$(implicit entry: Option[BibEntry]): TOption[String] =
    entry match {
      case Some(e) ⇒
        TSome(e.key)
      case None ⇒
        TError("There is no current entry, unable to execute the `cite$' function")
    }

  /**
   * extract the `authorNb'-th name of string `authorList' (in which names are
   * separated by and), and formats it according to specification
   * given by string `pattern'.
   */
  def formatName$(pattern: String, authorNb: Int, authorList: String) = {
    // extract author names
    val list = AuthorNamesExtractor.toList(authorList)
    if (list.size > authorNb) {
      // get the formatter associated to the pattern
      try {
        val formatter = formatters.getOrElseUpdate(pattern, new NameFormatter(pattern))
        // returns the formatted name
        TSome(formatter(list(authorNb)))
      }
      catch {
        case e: Exception ⇒
          TError("Unable to call `format,name$' function:\n", e)
      }
    }
    else {
      // author does not exist
      TError(authorNb + "-th author does not exist in {" + authorList + "}")
    }
  }

  def numNames$(authorList: String) =
    AuthorNamesExtractor.authorNb(authorList)

  def purify$(s: String) = {

    def purifyWord(word: Word): String =
      word.letters.foldLeft("") { (result, current) ⇒
        val purified = current match {
          case CharacterLetter(c) if c.isLetterOrDigit ⇒ c
          case CharacterLetter('-')                    ⇒ " "
          case CharacterLetter('~')                    ⇒ " "
          case SpecialLetter(_, Some(arg), false)      ⇒ arg
          case BlockLetter(parts)                      ⇒ purifyWord(SimpleWord(parts))
          case _                                       ⇒ ""
        }
        result + purified
      }

    import StringUtils.StringParser
    StringParser.parseAll(StringParser.string, s) match {
      case StringParser.Success(res, _) ⇒
        TSome(res.map(purifyWord _).mkString(" "))
      case fail ⇒
        TError(fail.toString)
    }
  }

  def width$(s: String) = {
    def charWidth(c: Char) =
      if (c >= 0 && c < o"200")
        widths(c)
      else 0

    def letterWidth(l: PseudoLetter): Int = l match {
      case CharacterLetter(c) ⇒ charWidth(c)
      case BlockLetter(parts) ⇒
        parts.map(letterWidth _).sum // does not take braces into account
      case SpecialLetter("oe", _, _) ⇒ 778
      case SpecialLetter("OE", _, _) ⇒ 1014
      case SpecialLetter("ae", _, _) ⇒ 722
      case SpecialLetter("AE", _, _) ⇒ 903
      case SpecialLetter("ss", _, _) ⇒ 500
      case SpecialLetter(command, arg, _) if command(0).isLetter ⇒
        charWidth(command(0)) + arg.map(_.map(charWidth _).sum).getOrElse(0)
      case SpecialLetter(_, arg, _) ⇒
        arg.map(_.map(charWidth _).sum).getOrElse(0)
    }

    def wordWidth(w: Word): Int =
      w.letters.map(letterWidth _).sum

    import StringUtils.StringParser
    StringParser.parseAll(StringParser.string, s) match {
      case StringParser.Success(res, _) ⇒
        TSome(res.foldLeft(0) { (result, current) ⇒
          result + wordWidth(current)
        })
      case fail ⇒
        TError(fail.toString)
    }

  }

}