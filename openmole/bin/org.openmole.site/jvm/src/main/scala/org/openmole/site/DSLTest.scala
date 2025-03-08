/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

object DSLTest {

  //  def withTmpServices[T](f: Services => T) = {
  //    val tmpDirectory = {
  //      val newF = java.io.File.createTempFile("workspace", "")
  //      newF.delete()
  //      newF.mkdirs()
  //      newF
  //    }
  //
  //    def build(workspace: File, password: String) = {
  //      implicit val ws = Workspace(workspace)
  //      implicit val cypher = Cypher(password)
  //      implicit val preference = Services.preference(ws)
  //      implicit val newFile = NewFile(workspace)
  //      implicit val seeder = Seeder()
  //      implicit val serializerService = SerializerService()
  //      implicit val threadProvider = ThreadProvider()
  //      implicit val replicaCatalog = ReplicaCatalog(org.openmole.core.db.memory())
  //      implicit val authenticationStore = Services.authenticationStore(ws)
  //      implicit val fileService = FileService()
  //      implicit val randomProvider = RandomProvider(seeder.newRNG)
  //      implicit val eventDispatcher = EventDispatcher()
  //      new ServicesContainer()
  //    }
  //
  //    val services = build(tmpDirectory, "")
  //
  //    try f(services)
  //    finally {
  //      Services.dispose(services)
  //      tmpDirectory.recursiveDelete
  //    }
  //  }
  //
  //  case class Test(code: String, header: String, number: Int) {
  //    def toCode =
  //      s"""def test${number}: Unit = {
  //$header
  //$code
  //}"""
  //  }
  //
  //  val toTest = ListBuffer[Test]()
  //
  //  def test(code: String, header: String) = toTest.synchronized {
  //    toTest += Test(code, header, toTest.size)
  //  }
  //
  //  def testCode = toTest.map { _.toCode }.mkString("\n")
  //
  //  def runTest =
  //    Try {
  //      withTmpServices { implicit services =>
  //        val repl = Project.newREPL(ConsoleVariables.empty)
  //        repl.compile(testCode)
  //      }
  //    }
  //
  //  def clear = toTest.clear

}
