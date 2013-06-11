/*
 * Copyright (C) 10/06/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.environment.{ BatchEnvironment, SerializedJob }
import java.io.{ OutputStream, PrintStream }
import java.util.UUID
import java.net.URI
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.storage.StorageService

trait JobScript {

  def environment: BatchEnvironment {
    def voName: String
  }

  protected def generateScript(
    serializedJob: SerializedJob,
    resultPath: String,
    runningPath: Option[String],
    finishedPath: Option[String],
    os: OutputStream) = {
    import serializedJob._

    val writter = new PrintStream(os)

    assert(runtime.runtime.path != null)

    runningPath.foreach { path ⇒ writter.print(touch(storage.url.resolve(path))) }
    writter.print("BASEPATH=$PWD; CUR=$PWD/ws$RANDOM; while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; export OPENMOLE_HOME=$CUR; ")
    writter.print("if [ `uname -m` = x86_64 ]; then ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz")) //, homeCacheDir, runtime.jvmLinuxX64.hash))
    writter.print("; else ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz")) //, homeCacheDir, runtime.jvmLinuxI386.hash))
    writter.print("; fi; ")
    writter.print("tar -xzf jvm.tar.gz >/dev/null; rm -f jvm.tar.gz; ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.runtime.path), "$PWD/openmole.tar.gz")) //, homeCacheDir, runtime.runtime.hash))
    writter.print("; tar -xzf openmole.tar.gz >/dev/null; rm -f openmole.tar.gz; ")
    writter.print("mkdir envplugins; PLUGIN=0;")

    for (plugin ← runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(lcgCpGunZipCmd(storage.url.resolve(plugin.path), "$CUR/envplugins/plugin$PLUGIN.jar")) //, homeCacheDir, plugin.hash))
      writter.print("; PLUGIN=`expr $PLUGIN + 1`; ")
    }

    writter.print(lcgCpCmd(storage.url.resolve(runtime.storage.path), "$CUR/storage.xml.gz"))

    writter.print(" ; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
    writter.print(environment.openMOLEMemoryValue)
    writter.print("m ")
    writter.print(UUID.randomUUID)
    writter.print(" -c ")
    writter.print(path)
    writter.print(" -s $CUR/storage.xml.gz ")
    writter.print(" -p $CUR/envplugins/ ")
    writter.print(" -i ")
    writter.print(inputFile)
    writter.print(" -o ")
    writter.print(resultPath)
    writter.print(" -t ")
    writter.print(environment.threadsValue)
    writter.print("; ")
    finishedPath.foreach { path ⇒ writter.print(touch(storage.url.resolve(path))) }
    writter.print("cd .. ; rm -rf $CUR ; ")
  }

  protected def touch(dest: URI) = {
    val name = UUID.randomUUID.toString
    s"touch $name; ${lcgCpCmd(name, dest)}; rm $name; "
  }

  protected def lcgCpGunZipCmd(from: URI, to: String) = {
    val builder = new StringBuilder
    builder.append(lcgCpCmd(from, to + ".gz"))
    builder.append(" && gunzip ")
    builder.append(to)
    builder.append(".gz ")
    builder.toString
  }

  @transient lazy val lcgCp =
    s"lcg-cp --vo ${environment.voName} --checksum --connect-timeout $getTimeOut --sendreceive-timeout $getTimeOut --srm-timeout $getTimeOut "

  protected def lcgCpCmd(from: String, to: URI) = s"$lcgCp file:$from ${to.toString}"
  protected def lcgCpCmd(from: URI, to: String) = s"$lcgCp ${from.toString} file:$to"

  private def getTimeOut = Workspace.preferenceAsDuration(GliteEnvironment.RemoteTimeout).toSeconds.toString

}
