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
package renderer

sealed trait Filter {
  /** Does the given entry match this filter? */
  def matches_?(entry: BibEntry): Boolean
}

case class Or(left: Filter, right: Filter) extends Filter {
  def matches_?(entry: BibEntry) = left.matches_?(entry) || right.matches_?(entry)
}
case class And(left: Filter, right: Filter) extends Filter {
  def matches_?(entry: BibEntry) = left.matches_?(entry) && right.matches_?(entry)
}
case class Not(filter: Filter) extends Filter {
  def matches_?(entry: BibEntry) = !filter.matches_?(entry)
}
case class FieldIs(fieldName: String, fieldValue: Value) extends Filter {
  def matches_?(entry: BibEntry) =
    entry.fields.get(fieldName).filter(_.value == fieldValue).isDefined
}
case class FieldLess(fieldName: String, fieldValue: Value) extends Filter {
  def matches_?(entry: BibEntry) =
    entry.fields.get(fieldName).filter(_.value <= fieldValue).isDefined
}
case class FieldMore(fieldName: String, fieldValue: Value) extends Filter {
  def matches_?(entry: BibEntry) =
    entry.fields.get(fieldName).filter(_.value >= fieldValue).isDefined
}
case class EntryIs(entryType: String) extends Filter {
  def matches_?(entry: BibEntry) =
    entry.name == entryType.toLowerCase
}
private[bibtex] object TrueFilter extends Filter {
  def matches_?(entry: BibEntry) = true
}