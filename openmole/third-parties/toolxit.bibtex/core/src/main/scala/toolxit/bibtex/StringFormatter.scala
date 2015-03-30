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

/**
 * A formatter allows to format a string represented as a list of words.
 *
 * @author Lucas Satabin
 *
 */
trait StringFormatter extends (String ⇒ String) {
  def apply(string: String): String = {
    import StringUtils.StringParser
    StringParser.parseAll(StringParser.string, string) match {
      case StringParser.Success(res, _) ⇒ this(res).mkString(" ")
      case _                            ⇒ string
    }
  }
  def apply(string: List[Word]): List[Word]
}

object StringFormatters {

  /**
   * Flattens all blocks in the string by simply removing the braces.
   * For special characters, if it is a known special, it is replaced by the
   * corresponding UTF-8 encoded character, otherwise, the command is simply
   * removed, and only the argument is kept
   */
  lazy val flatten = new StringFormatter {
    def apply(string: List[Word]) = {
      def removeBlock(letter: PseudoLetter): List[PseudoLetter] = letter match {
        case BlockLetter(parts) ⇒
          parts.flatMap(removeBlock _)
        case s: SpecialLetter if s.toUTF8.isDefined ⇒ List(s.toUTF8.get)
        case SpecialLetter(_, Some(arg), _)         ⇒ new SimpleWord(arg).letters
        case _: SpecialLetter                       ⇒ Nil // unknown and no argument -> remove it...
        case l                                      ⇒ List(l) // otherwise just return the letter
      }
      def applyWord(word: Word): Word = word match {
        case SimpleWord(letters) ⇒
          val res = letters.flatMap(removeBlock _)
          SimpleWord(res)
        case ComposedWord(first, second, sep) ⇒
          ComposedWord(applyWord(first), applyWord(second), sep)
      }
      string.map(applyWord _)
    }
  }

  /** Turns into upper case all characters at brace level zero. */
  lazy val toUpper = new StringFormatter {
    def apply(string: List[Word]) = {
      def toUpper(letter: PseudoLetter) = letter match {
        case CharacterLetter(c) ⇒ CharacterLetter(c.toUpper)
        case SpecialLetter(command, arg, braces) ⇒
          SpecialLetter(command, arg.map(_.toUpperCase), braces)
        case block ⇒ block
      }
      def applyWord(word: Word): Word = word match {
        case SimpleWord(letters) ⇒
          SimpleWord(letters.map(toUpper _))
        case ComposedWord(first, second, sep) ⇒
          ComposedWord(applyWord(first), applyWord(second), sep)
      }
      string.map(applyWord _)
    }
  }

  /** Turns into lower case all characters at brace level zero. */
  lazy val toLower = new StringFormatter {
    def apply(string: List[Word]) = {
      def toLower(letter: PseudoLetter) = letter match {
        case CharacterLetter(c) ⇒ CharacterLetter(c.toLower)
        case SpecialLetter(command, arg, braces) ⇒
          SpecialLetter(command, arg.map(_.toLowerCase), braces)
        case block ⇒ block
      }
      def applyWord(word: Word): Word = word match {
        case SimpleWord(letters) ⇒
          SimpleWord(letters.map(toLower _))
        case ComposedWord(first, second, sep) ⇒
          ComposedWord(applyWord(first), applyWord(second), sep)
      }
      string.map(applyWord _)
    }
  }

  /**
   * Turns into lower case all characters at brace level zero except
   * for the first letter.
   */
  lazy val toLowerButFirst = new StringFormatter {
    def apply(string: List[Word]) = {
      def toLower(letter: PseudoLetter) = letter match {
        case CharacterLetter(c) ⇒ CharacterLetter(c.toLower)
        case SpecialLetter(command, arg, braces) ⇒
          SpecialLetter(command, arg.map(_.toLowerCase), braces)
        case block ⇒ block
      }
      def applyWord(word: Word): Word = word match {
        case SimpleWord(letters) ⇒
          SimpleWord(letters.map(toLower _))
        case ComposedWord(first, second, sep) ⇒
          ComposedWord(applyWord(first), applyWord(second), sep)
      }
      def applyFirst(word: Word): Word = word match {
        case SimpleWord(letters) ⇒
          SimpleWord(letters.head :: letters.tail.map(toLower _))
        case ComposedWord(first, second, sep) ⇒
          ComposedWord(applyFirst(first), applyWord(second), sep)
      }
      applyFirst(string.head) :: string.tail.map(applyWord _)
    }
  }

}
