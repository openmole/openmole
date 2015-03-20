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

import java.io.Reader
import collection.mutable.ListBuffer

/**
 * @author Lucas Satabin
 *
 */
final case class AuxFile(style: Option[String], citations: List[String])

object AuxReader {
  def read(reader: Reader) = {
    val buffered = new java.io.BufferedReader(reader)
    var line = buffered.readLine
    val buffer = new ListBuffer[String]
    while (line != null) {
      buffer += line
      line = buffered.readLine
    }
    buffer.toList
  }

}