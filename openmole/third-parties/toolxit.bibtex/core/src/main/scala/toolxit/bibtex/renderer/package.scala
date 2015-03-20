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
package object renderer {

  implicit val defaultEnvironment: Map[String, String] =
    Map(
      "jan" -> "January",
      "feb" -> "February",
      "mar" -> "March",
      "apr" -> "April",
      "may" -> "May",
      "jun" -> "June",
      "jul" -> "July",
      "aug" -> "August",
      "sep" -> "September",
      "oct" -> "October",
      "nov" -> "November",
      "dec" -> "December",
      "acmcs" -> "ACM Computing Surveys",
      "acta" -> "Acta Informatica",
      "cacm" -> "Communications of the ACM",
      "ibmjrd" -> "IBM Journal of Research and Development",
      "ibmsj" -> "IBM Systems Journal",
      "ieeese" -> "IEEE Transactions on Software Engineering",
      "ieeetc" -> "IEEE Transactions on Computers",
      "ieeetcad" ->
        "IEEE Transactions on Computer-Aided Design of Integrated Circuits",
      "ipl" -> "Information Processing Letters",
      "jacm" -> "Journal of the ACM",
      "jcss" -> "Journal of Computer and System Sciences",
      "scp" -> "Science of Computer Programming",
      "sicomp" -> "SIAM Journal on Computing",
      "tocs" -> "ACM Transactions on Computer Systems",
      "tods" -> "ACM Transactions on Database Systems",
      "tog" -> "ACM Transactions on Graphics",
      "toms" -> "ACM Transactions on Mathematical Software",
      "toois" -> "ACM Transactions on Office Information Systems",
      "toplas" -> "ACM Transactions on Programming Languages and Systems",
      "tcs" -> "Theoretical Computer Science")

}