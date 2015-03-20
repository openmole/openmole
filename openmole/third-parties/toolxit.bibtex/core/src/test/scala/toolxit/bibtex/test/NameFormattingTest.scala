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
package test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NameFormattingTest extends FlatSpec with ShouldMatchers {

  "The formatter" should "format correctly" in {
    val formatter = new NameFormatter("{f. }{vv }{ll}")
    formatter(Author("Lucas", "", "Satabin", "")) should equal("L. Satabin")
    formatter(Author("Jean-Baptiste", "", "Poquelin", "")) should equal("J.-B. Poquelin")
  }

}