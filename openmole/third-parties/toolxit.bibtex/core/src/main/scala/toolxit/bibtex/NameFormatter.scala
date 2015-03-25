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
 * This class creates a name formatter with the given patter.
 * It may be reused once the pattern is compiled for efficiency reasons.
 *
 * @author Lucas Satabin
 *
 */
class NameFormatter(pattern: String) extends (Author ⇒ String) {

  // compile the pattern
  import PatternParser._
  /*private[this]*/ val parts = parseAll(PatternParser.pattern, pattern) match {
    case Success(res, _) ⇒ res
    case f               ⇒ throw new Exception(f.toString)
  }

  /**
   * Formats the author according to the given pattern.
   */
  def apply(author: Author): String = {
    parts.foldLeft("") { (result, current) ⇒
      val cur = current match {
        case FirstPattern(true, before, sep, after) if author.first.nonEmpty ⇒
          format(before.getOrElse(""),
            author.first,
            sep,
            after.getOrElse(""))
        case FirstPattern(false, before, sep, after) if author.first.nonEmpty ⇒
          format(before.getOrElse(""),
            shorten(author.first, sep),
            sep,
            after.getOrElse(""))
        case LastPattern(true, before, sep, after) if author.last.nonEmpty ⇒
          format(before.getOrElse(""),
            author.last,
            sep,
            after.getOrElse(""))
        case LastPattern(false, before, sep, after) if author.last.nonEmpty ⇒
          format(before.getOrElse(""),
            shorten(author.last, sep),
            sep,
            after.getOrElse(""))
        case VonPattern(true, before, sep, after) if author.von.nonEmpty ⇒
          format(before.getOrElse(""),
            author.von,
            sep,
            after.getOrElse(""))
        case VonPattern(false, before, sep, after) if author.von.nonEmpty ⇒
          format(before.getOrElse(""),
            shorten(author.von, sep),
            sep,
            after.getOrElse(""))
        case JrPattern(true, before, sep, after) if author.jr.nonEmpty ⇒
          format(before.getOrElse(""),
            author.jr,
            sep,
            after.getOrElse(""))
        case JrPattern(false, before, sep, after) if author.jr.nonEmpty ⇒
          format(before.getOrElse(""),
            shorten(author.jr, sep),
            sep,
            after.getOrElse(""))
        case Other(text) ⇒ text
        case _           ⇒ ""
      }
      result + cur
    }
  }

  /**
   * Returns the shortened version of this name.
   * For example (with `.' as separator)
   * <ul>
   * <li>`Lucas' is shortened as `L'</li>
   * <li>`Jean-Baptiste' is shortened as `J.-B'</li>
   * </ul>
   */
  def shorten(name: List[Word], sep: String) = {
    def shortenOne(w: Word): List[PseudoLetter] = w match {
      case SimpleWord((first: CharacterLetter) :: _)         ⇒ List(first)
      case SimpleWord(SpecialLetter(_, Some(first), _) :: _) ⇒ shortenOne(new SimpleWord(first))
      case SimpleWord(SpecialLetter(_, None, _) :: tail)     ⇒ shortenOne(SimpleWord(tail))
      case SimpleWord(BlockLetter(Nil) :: tail)              ⇒ shortenOne(SimpleWord(tail))
      case SimpleWord(BlockLetter(letters) :: _)             ⇒ letters
      case ComposedWord(first, second, s) ⇒
        shortenOne(first) ++ (new SimpleWord(sep).letters) ++ List(s) ++ shortenOne(second)
      case _ ⇒
        Nil
    }
    name.map(w ⇒ SimpleWord(shortenOne(w)))
  }

  // ====== helper methods ======

  @inline
  private def format(before: String, values: List[Word], sep: String, after: String) =
    values.mkString(before, sep, after)

}

sealed trait Part
sealed abstract class Pattern extends Part {
  val full: Boolean
  val before: Option[String]
  val separator: String
  val after: Option[String]
}
object Pattern {
  def unapply(p: Pattern) =
    Some((p.full, p.before, p.separator, p.after))
}
case class FirstPattern(full: Boolean,
                        before: Option[String],
                        separator: String,
                        after: Option[String]) extends Pattern
case class LastPattern(full: Boolean,
                       before: Option[String],
                       separator: String,
                       after: Option[String]) extends Pattern
case class VonPattern(full: Boolean,
                      before: Option[String],
                      separator: String,
                      after: Option[String]) extends Pattern
case class JrPattern(full: Boolean,
                     before: Option[String],
                     separator: String,
                     after: Option[String]) extends Pattern
case class Other(text: String) extends Part

object PatternParser extends RegexParsers {

  override def skipWhitespace = false

  lazy val pattern: Parser[List[Part]] =
    rep(firstPattern | lastPattern | vonPattern | jrPattern | other)

  lazy val firstPattern: Parser[FirstPattern] =
    // long name
    part("ff") ^^ partCreator(true, FirstPattern) |
      // short name
      part("f") ^^ partCreator(false, FirstPattern)

  lazy val lastPattern: Parser[LastPattern] =
    // long name
    part("ll") ^^ partCreator(true, LastPattern) |
      // short name
      part("l") ^^ partCreator(false, LastPattern)

  lazy val vonPattern: Parser[VonPattern] =
    // long name
    part("vv") ^^ partCreator(true, VonPattern) |
      // short name
      part("v") ^^ partCreator(false, VonPattern)

  lazy val jrPattern: Parser[JrPattern] =
    // long name
    part("jj") ^^ partCreator(true, JrPattern) |
      // short name
      part("j") ^^ partCreator(false, JrPattern)

  lazy val other: Parser[Other] = uptoBlock

  lazy val block: Parser[String] =
    "{" ~> rep("[^}{]+".r | block) <~ "}" ^^ (_.mkString("{", "", "}"))

  lazy val sepblock: Parser[String] =
    "{" ~> rep("[^}{]+".r | block) <~ "}" ^^ (_.mkString)

  lazy val uptoBlock =
    guard("{") ~> "" ^^^ Other("") |
      rep1(not("{") ~> ".".r) ^^ (list ⇒ Other(list.mkString))

  // ====== helper methods ======

  def uptoPart(partName: String) =
    guard(partName) ^^^ None |
      rep1(not(partName) ~> ".".r) ^^ (s ⇒ Some(s.mkString))

  /* create a parser for a part */
  private def part(partName: String) =
    "{" ~> (opt(block) | uptoPart(partName)) ~ (partName ~> opt(sepblock)) ~ opt(block | "[^{}]*".r) <~ "}"

  /* takes a parsed part and creates the associated node */
  private def partCreator[T <: Pattern](full: Boolean, creator: (Boolean, Option[String], String, Option[String]) ⇒ T) =
    (input: ~[~[Option[String], Option[String]], Option[String]]) ⇒
      input match {
        case before ~ Some(sep) ~ Some(after) ⇒
          creator(full, before, sep, Some(after))
        case before ~ Some(sep) ~ None ⇒
          creator(full, before, sep, Some(sep))
        case before ~ None ~ after ⇒
          creator(full, before, if (full) " " else ".", after)
      }

}
