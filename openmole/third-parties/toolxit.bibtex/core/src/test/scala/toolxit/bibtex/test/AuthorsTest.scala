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
class AuthorsTest extends FlatSpec with ShouldMatchers {

  @inline
  def parseAuthor(input: String) =
    AuthorNameExtractor.parse(input)

  "The parser" should "correctly parse author names, and split the different parts in First von Last format" in {

    // first von last form
    parseAuthor("AA BB") should equal(Author("AA", "", "BB", ""))
    parseAuthor("AA") should equal(Author("", "", "AA", ""))
    parseAuthor("AA bb") should equal(Author("AA", "", "bb", ""))
    parseAuthor("aa") should equal(Author("", "", "aa", ""))
    parseAuthor("AA bb CC") should equal(Author("AA", "bb", "CC", ""))
    parseAuthor("AA bb CC dd EE") should equal(Author("AA", "bb CC dd", "EE", ""))
    parseAuthor("AA 1B cc dd") should equal(Author("AA 1B", "cc", "dd", ""))
    parseAuthor("AA 1b cc dd") should equal(Author("AA", "1b cc", "dd", ""))
    parseAuthor("AA {b}B cc dd") should equal(Author("AA {b}B", "cc", "dd", ""))
    parseAuthor("AA {b}b cc dd") should equal(Author("AA", "{b}b cc", "dd", ""))
    parseAuthor("AA {B}b cc dd") should equal(Author("AA", "{B}b cc", "dd", ""))
    parseAuthor("AA {B}B cc dd") should equal(Author("AA {B}B", "cc", "dd", ""))
    parseAuthor("AA \\BB{b} cc dd") should equal(Author("AA \\BB{b}", "cc", "dd", ""))
    parseAuthor("AA \\bb{b} cc dd") should equal(Author("AA", " \\bb{b} cc", "dd", ""))
    parseAuthor("AA {bb} cc DD") should equal(Author("AA {bb}", "cc", "DD", ""))
    parseAuthor("AA bb {cc} DD") should equal(Author("AA", "bb", "{cc} DD", ""))
    parseAuthor("AA {bb} CC") should equal(Author("AA {bb}", "", "CC", ""))

  }

  it should "correctly parse author names, and split the different parts in von Last, Jr, First format" in {
    // von last, jr, first form
    parseAuthor("bb CC, AA") should equal(Author("AA", "bb", "CC", ""))
    parseAuthor("bb CC, aa") should equal(Author("aa", "bb", "CC", ""))
    parseAuthor("bb CC dd EE, AA") should equal(Author("AA", "bb CC dd", "EE", ""))
    parseAuthor("bb, AA") should equal(Author("AA", "", "bb", ""))
    parseAuthor("BB,") should equal(Author("", "", "BB", ""))
    parseAuthor("bb CC,XX, AA") should equal(Author("AA", "bb", "CC", "XX"))
    parseAuthor("bb CC,xx, AA") should equal(Author("AA", "bb", "CC", "xx"))
    parseAuthor("BB,, AA") should equal(Author("AA", "", "BB", ""))

  }

  it should "correctly recognize special characters and their case in First von Last format" in {
    parseAuthor("Paul \\'Emile Victor") should equal(Author("Paul \\'Emile", "", "Victor", ""))
    parseAuthor("Paul {\\'E}mile Victor") should equal(Author("Paul {\\'E}mile", "", "Victor", ""))
    parseAuthor("Paul \\'emile Victor") should equal(Author("Paul", "\\'emile", "Victor", ""))
    parseAuthor("Paul {\\'e}mile Victor") should equal(Author("Paul", "{\\'e}mile", "Victor", ""))

  }

  it should "correctly recognize special characters and their case in von Last, Jr, First format" in {
    parseAuthor("Victor, Paul \\'Emile") should equal(Author("Paul \\'Emile", "", "Victor", ""))
    parseAuthor("Victor, Paul {\\'E}mile") should equal(Author("Paul {\\'E}mile", "", "Victor", ""))
    parseAuthor("Victor, Paul \\'emile") should equal(Author("Paul \\'emile", "", "Victor", ""))
    parseAuthor("Victor, Paul {\\'e}mile") should equal(Author("Paul {\\'e}mile", "", "Victor", ""))
  }

  it should "correctly recognize the first level 0 letter in First von Last format" in {
    parseAuthor("Dominique Galouzeau de Villepin") should equal(Author("Dominique Galouzeau", "de", "Villepin", ""))
    parseAuthor("Dominique {G}alouzeau de Villepin") should equal(Author("Dominique", "{G}alouzeau de", "Villepin", ""))
    "Galouzeau de Villepin, Dominique"
  }

  it should "correctly recognize the first level 0 letter in von Last, Jr, First format" in {
    parseAuthor("Galouzeau de Villepin, Dominique") should equal(Author("Dominique", "", "Galouzeau de Villepin", ""))
    parseAuthor("{G}alouzeau de Villepin, Dominique") should equal(Author("Dominique", "{G}alouzeau de", "Villepin", ""))
  }

}

object Toto extends App {
  println(AuthorNameExtractor.parse("Doppler, {\\relax Ch}ristian Andreas"))
}