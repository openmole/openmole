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

case class Author(first: List[Word],
                  von: List[Word],
                  last: List[Word],
                  jr: List[Word]) {

  def this(first: String, von: String, last: String, jr: String) =
    this(StringUtils.StringParser.parseAll(StringUtils.StringParser.string, first).get,
      StringUtils.StringParser.parseAll(StringUtils.StringParser.string, von).get,
      StringUtils.StringParser.parseAll(StringUtils.StringParser.string, last).get,
      StringUtils.StringParser.parseAll(StringUtils.StringParser.string, jr).get)

  override def toString =
    "first: " + first +
      "\nvon: " + von +
      "\nlast: " + last +
      "\njr: " + jr

  override def equals(other: Any) = other match {
    case Author(f, v, l, j) ⇒
      first == f && v == von && l == last && j == jr
    case _ ⇒ false
  }

  override def hashCode = {
    var hash = 31 + first.hashCode
    hash = hash * 31 + von.hashCode
    hash = hash * 31 + last.hashCode
    hash = hash * 31 + jr.hashCode
    hash
  }

}

object Author {
  def apply(first: String, von: String, last: String, jr: String): Author =
    new Author(first.trim, von.trim, last.trim, jr.trim)
}