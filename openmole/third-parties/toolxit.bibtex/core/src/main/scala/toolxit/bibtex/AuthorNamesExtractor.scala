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
 * Utilities to extract the name of the different authors defined in a string.
 * @author Lucas Satabin
 *
 */
object AuthorNamesExtractor extends StringUtils.StringParser {

  lazy val nameSep = """(?i)\s+and\s+""".r

  lazy val names =
    rep1sep(uptoNameSep, nameSep) ^^ (_.map(_.toString))

  lazy val uptoNameSep =
    guard(nameSep) ~> "" ^^^ SimpleWord(Nil) |
      rep1(block | special | not(nameSep) ~> ".|\\s".r ^^
        (str ⇒ CharacterLetter(str.charAt(0)))) ^^ SimpleWord

  def toList(authors: String) = {
    parseAll(names, authors).getOrElse(Nil).map { author ⇒
      try {
        AuthorNameExtractor.parse(author)
      }
      catch {
        case e: Exception ⇒
          println("Wrong author format: " + author)
          println(e.getMessage)
          println("This author is omitted")
          EmptyAuthor
      }
    }
  }

  def authorNb(authors: String) =
    parseAll(names, authors) match {
      case Success(res, _) ⇒ TSome(res.size)
      case failure         ⇒ TError(failure.toString)
    }

}