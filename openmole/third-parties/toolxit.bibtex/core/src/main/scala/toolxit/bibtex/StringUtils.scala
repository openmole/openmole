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
import scala.annotation.tailrec

/**
 * @author Lucas Satabin
 *
 */
object StringUtils {

  implicit def char2testable(c: Char) = new {
    def isAlphaNumeric = c.toString.matches("[a-zA-Z0-9]")
    def isBibTeXLower =
      if (c.isDigit)
        true
      else
        c.isLower
  }

  object StringParser extends StringParser
  class StringParser extends RegexParsers {

    override def skipWhitespace = false

    lazy val string: Parser[List[Word]] =
      repsep(word | "," ^^^ SimpleWord(List(CharacterLetter(','))), "\\s+".r)

    lazy val word: Parser[Word] = composedword | simpleword

    lazy val simpleword: Parser[SimpleWord] = rep1(pseudoLetter) ^^ SimpleWord

    lazy val composedword: Parser[ComposedWord] =
      simpleword ~ sep ~ word ^^ {
        case first ~ sep ~ second ⇒ ComposedWord(first, second, sep)
      }

    lazy val pseudoLetter: Parser[PseudoLetter] = special | block | character

    lazy val character: Parser[CharacterLetter] =
      "[^-~\\{}\\s,]".r ^^ (s ⇒ CharacterLetter(s.charAt(0)))

    lazy val sep: Parser[CharacterLetter] =
      "[-~]".r ^^ (s ⇒ CharacterLetter(s.charAt(0)))

    lazy val block: Parser[BlockLetter] =
      "{" ~>
        rep(block | character
          | "\\s".r ^^ (s ⇒ CharacterLetter(s.charAt(0)))) <~ "}" ^^ BlockLetter

    lazy val special: Parser[SpecialLetter] =
      "{\\" ~> ("'|\"|´|`|\\^|~|[^\\s{}'\"´`^~]+".r <~ "\\s*".r) ~
        opt(block ^^ (s ⇒ (true, s.parts.mkString))
          | ("\\s*[^{}\\s]+\\s*".r ^^ (s ⇒ (false, s.trim)))) <~ "}" ^^ {
          case spec ~ Some((braces, char)) ⇒ SpecialLetter(spec, Some(char), braces)
          case spec ~ None                 ⇒ SpecialLetter(spec, None, false)
        }

  }

  /* returns the first non brace character at level 0 if any */
  def firstCharacter(str: Word): Option[Char] = {
    @tailrec
    def findFirst(letters: List[PseudoLetter]): Option[Char] = letters match {
      case (_: BlockLetter) :: tail ⇒
        findFirst(tail)
      case SpecialLetter(spec, _, _) :: _ if spec.contains((c: Char) ⇒ c.isLetter) ⇒
        spec.find(_.isLetter)
      case SpecialLetter(_, Some(char), _) :: _ ⇒
        char.find(_.isAlphaNumeric)
      case CharacterLetter(c) :: _ if c.isLetter ⇒
        Some(c)
      case _ :: tail ⇒
        findFirst(tail)
      case Nil ⇒ None
    }
    findFirst(str.letters)
  }

  def isFirstCharacterLower(str: Word) =
    firstCharacter(str).map(_.isBibTeXLower).getOrElse(false)

}

sealed trait PseudoLetter {
  val whitespace_? : Boolean
}
final case class CharacterLetter(char: Char) extends PseudoLetter {
  override def toString = char.toString
  val whitespace_? = char.toString.matches("\\s+")
}
final case class BlockLetter(parts: List[PseudoLetter]) extends PseudoLetter {
  override def toString = parts.mkString("{", "", "}")
  val whitespace_? = parts.forall(_.whitespace_?)
}
final case class SpecialLetter(command: String, arg: Option[String], withBraces: Boolean) extends PseudoLetter {
  override def toString = {
    val argument = arg match {
      case Some(a) if withBraces ⇒ "{" + a + "}"
      case Some(a)               ⇒ a
      case None                  ⇒ ""
    }
    "{\\" + command + argument + "}"
  }

  /** Returns the UTF8 representation of this special letter if known */
  def toUTF8: Option[CharacterLetter] = SpecialCharacters(this).map(CharacterLetter)

  val whitespace_? = false

}
trait Word {
  val letters: List[PseudoLetter]
  val length: Int
}
final case class ComposedWord(first: Word, second: Word, sep: CharacterLetter) extends Word {
  val letters = first.letters ++ List(sep) ++ second.letters
  val length = first.length + second.length + 1
  override def toString = "" + first + sep + second
}
final case class SimpleWord(letters: List[PseudoLetter]) extends Word {
  def this(str: String) = this(str.toCharArray.map(CharacterLetter).toList)
  val length = letters.foldLeft(0) { (result, current) ⇒
    def internalCount(letter: PseudoLetter, depth: Int): Int = letter match {
      case _: CharacterLetter ⇒ 1
      case _: SpecialLetter if depth == 0 ⇒
        // only special characters at brace level 0 count
        1
      case BlockLetter(parts) ⇒
        parts.map(internalCount(_, depth + 1)).sum
      case _ ⇒ 0
    }
    result + internalCount(current, 0)
  }
  override def toString = letters.mkString
}
final case class Sentence(words: List[Word]) {
  override def toString = words.mkString(" ")
}