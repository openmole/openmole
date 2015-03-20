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

import scala.util.parsing.combinator.RegexParsers

/**
 *
 * A collection of parsers to parse BibTeX files.
 * This is a strict parser, there is no recovery on error if the BibTeX
 * file is not syntactically correct.
 *
 * @author Lucas Satabin
 *
 */
object BibTeXParsers extends RegexParsers {

  // extends the whiteSpace value to handle comments
  protected override val whiteSpace = "(\\s|%.*)+".r

  /**
   * A .bib file consists in a list of:
   *  - string declarations
   *  - preamble declarations
   *  - comments (ignored)
   *  - bib entries
   */
  lazy val bibFile: Parser[BibTeXDatabase] = {
    rep(positioned(string | preamble | comment ^^^ null | entry))
  } ^^ { entries ⇒
    val grouped = entries.filter(_ != null).groupBy {
      case _: BibEntry      ⇒ "bib"
      case _: StringEntry   ⇒ "string"
      case _: PreambleEntry ⇒ "preamble"
    }
    BibTeXDatabase(grouped.getOrElse("bib", Nil).map(_.asInstanceOf[BibEntry]),
      grouped.getOrElse("string", Nil).map(_.asInstanceOf[StringEntry]),
      grouped.getOrElse("preamble", Nil).map(_.asInstanceOf[PreambleEntry]))
  }

  lazy val string: Parser[StringEntry] =
    ci("@string") ~> "{" ~> (name <~ "=") ~ quoted <~ "}" ^^ {
      case name ~ value ⇒ StringEntry(name, value)
    }

  lazy val preamble: Parser[PreambleEntry] =
    ci("@preamble") ~> "{" ~> concat <~ "}" ^^ PreambleEntry

  lazy val comment: Parser[Unit] =
    ci("@comment") ~> "{" ~> (string | preamble | entry) <~ "}" ^^^ {}

  lazy val entry: Parser[BibEntry] =
    ("@" ~> name <~ "{") ~ (key <~ ",") ~ repsep(positioned(field), ",") <~ opt(",") <~ "}" ^^ {
      case name ~ key ~ fields ⇒ BibEntry(name.toLowerCase, key,
        fields.map(f ⇒ (f.name, f)).toMap)
    }

  lazy val field: Parser[Field] =
    (name <~ "=") ~ value ^^ {
      case n ~ v ⇒ Field(n, v)
    }

  lazy val name: Parser[String] =
    "[^=\\s,{']+".r ^^ (_.toLowerCase)

  lazy val key: Parser[String] =
    "[^=\\s,{']+".r

  lazy val number: Parser[IntValue] = {
    "[0-9]+".r |
      "{" ~> "[0-9]+".r <~ "}" |
      "\"" ~> "[0-9]+".r <~ "\""
  } ^^ (s ⇒ IntValue(s.toInt))

  lazy val value: Parser[Value] = number | braced | concat | "[^\\s,]+".r ^^ StringValue

  lazy val someText = rep1("""[^\\}{"]""".r | escaped) ^^ (_.mkString(""))

  lazy val block: Parser[Value] = quoted | braced

  lazy val concatanable: Parser[Value] = quoted | name ^^ NameValue

  lazy val concat: Parser[ConcatValue] =
    concatanable ~ opt("#" ~> repsep(concatanable, "#")) ^^ {
      case first ~ Some(next) ⇒ ConcatValue(first :: next)
      case first ~ None       ⇒ ConcatValue(List(first))
    }

  lazy val quoted: Parser[StringValue] =
    enterBlock("\"") ~> rep(braced | someText) <~ leaveBlock("\"") ^^ { list ⇒
      val (start, end) =
        if (depth == 0)
          ("", "")
        else
          ("\"", "\"")
      StringValue(list.mkString(start, "", end))
    }

  lazy val braced: Parser[StringValue] =
    {
      enterBlock("{") ~> rep(braced | quoted | someText ^^ StringValue) <~ leaveBlock("}")
    } ^^ { list ⇒
      val (start, end) =
        if (depth == 0)
          ("", "")
        else
          ("{", "}")
      StringValue(list.map(_.value).mkString(start, "", end))
    }

  lazy val escaped: Parser[String] = "\\" ~> "[\\\"{}]".r

  // Helper methods
  private def ci(p: String): Parser[String] = ("(?i)" + p).r

  private var depth = 0

  // do not skip white spaces in a block
  override def skipWhitespace = depth == 0

  def andAction[T](after: ⇒ Unit)(p: ⇒ Parser[T]) =
    p ^^ (res ⇒ { after; res })

  def resetDepth[T](p: ⇒ Parser[T]) =
    andAction(depth = 0)(p)

  def enterBlock[T](p: ⇒ Parser[T]) =
    andAction(depth += 1)(p)

  def leaveBlock[T](p: ⇒ Parser[T]) =
    andAction(depth -= 1)(p)

}