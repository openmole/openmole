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
 * @author Lucas Satabin
 *
 */
object SpecialCharacters {

  private val withArg = Map(
    ("´", "a") -> 'á',
    ("'", "a") -> 'á',
    ("`", "a") -> 'à',
    ("\"", "a") -> 'ä',
    ("^", "a") -> 'â',
    ("~", "a") -> 'ã',
    ("´", "A") -> 'Á',
    ("'", "A") -> 'Á',
    ("`", "A") -> 'À',
    ("\"", "A") -> 'Ä',
    ("^", "A") -> 'Â',
    ("~", "A") -> 'Ã',
    ("´", "e") -> 'é',
    ("'", "e") -> 'é',
    ("`", "e") -> 'è',
    ("\"", "e") -> 'ë',
    ("^", "e") -> 'ê',
    ("~", "e") -> 'ẽ',
    ("c", "e") -> 'ȩ',
    ("´", "E") -> 'É',
    ("'", "E") -> 'É',
    ("`", "E") -> 'È',
    ("\"", "E") -> 'Ë',
    ("^", "E") -> 'Ê',
    ("~", "E") -> 'Ẽ',
    ("c", "E") -> 'Ȩ',
    ("c", "c") -> 'ç',
    ("c", "C") -> 'Ç',
    ("´", "o") -> 'ó',
    ("'", "o") -> 'ó',
    ("`", "o") -> 'ò',
    ("\"", "o") -> 'ö',
    ("^", "o") -> 'ô',
    ("~", "o") -> 'õ',
    ("´", "O") -> 'Ó',
    ("'", "O") -> 'Ó',
    ("`", "O") -> 'Ò',
    ("\"", "O") -> 'Ö',
    ("^", "O") -> 'Ô',
    ("~", "O") -> 'Õ')

  private val noArg = Map(
    "oe" -> 'œ',
    "OE" -> 'Œ',
    "ae" -> 'æ',
    "AE" -> 'Æ',
    "aa" -> 'å',
    "AA" -> 'Å',
    "o" -> 'ø',
    "O" -> 'Ø',
    "l" -> 'ł',
    "L" -> 'Ł',
    "ss" -> 'ß')

  def apply(special: SpecialLetter) = special match {
    case SpecialLetter(command, Some(arg), _) ⇒
      withArg.get((command, arg))
    case SpecialLetter(command, _, _) ⇒
      noArg.get(command)
    case _ ⇒ None
  }

}