/**
 * Created by Romain Reuillon on 22/09/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.core

import java.util.zip.GZIPInputStream
import org.openmole.tool.file.*
import org.openmole.tool.archive.*
import org.openmole.core.context.*
import org.openmole.core.argument.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.workspace.*
import org.openmole.tool.random.*
import org.openmole.tool.archive.*

import java.io.IOException
import org.openmole.core.exception.InternalProcessingError

package object market:

  lazy val marketIndexLocation = PreferenceLocation("Market", "Index", Some(buildinfo.marketAddress))

  import org.json4s._
  import org.json4s.jackson.Serialization
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  def indexURL(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    ExpandedString(preference(marketIndexLocation)).from(Context("version" → buildinfo.version))

  def marketIndex(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService, networkService: NetworkService) =
    Serialization.read[MarketIndex](NetworkService.get(indexURL))

  def downloadEntry(entry: MarketIndexEntry, path: File)(implicit networkService: NetworkService) = try {
    val tis = TarArchiveInputStream(new GZIPInputStream(NetworkService.getInputStream(entry.url)))
    try tis.extract(path)
    finally tis.close()
    path.applyRecursive(_.setExecutable(true))
  }
  catch {
    case e: IOException => throw new InternalProcessingError(s"Cannot download entry at url ${entry.url}", e)
  }


