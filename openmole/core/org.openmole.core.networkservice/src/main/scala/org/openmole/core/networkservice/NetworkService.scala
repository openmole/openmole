/*
 * Copyright (C) 2018 Samuel Thiriot
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

package org.openmole.core.networkservice

# TODO !!!!!!!!!!!!!!!

import java.io.File
import java.util.concurrent.TimeUnit

import com.google.common.cache._
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.threadprovider.{ ThreadProvider, Updater }
import org.openmole.tool.hash._
import org.openmole.core.workspace._
import org.openmole.tool.cache.AssociativeCache
import org.openmole.tool.hash._
import org.openmole.tool.tar._
import org.openmole.tool.file._
import org.openmole.tool.thread._
import squants._
import squants.time.TimeConversions._

import scala.collection.mutable.{ ListBuffer, WeakHashMap }
import scala.ref.WeakReference

object NetworkService {
  val ProxyEnabled =  ConfigurationLocation("NetworkService", "NetworkService", Some(False))
  val ProxyHost =     ConfigurationLocation("NetworkService", "ProxyHost", None)
  val ProxyPort =     ConfigurationLocation("NetworkService", "ProxyPort", None)
  val ProxyScheme =   ConfigurationLocation("NetworkService", "ProxyScheme", None)

  def apply()(implicit preference: Preference, threadProvider: ThreadProvider) = {
    val ns = new NetworkService
    ns.start
    ns
  }
}

class NetworkService(implicit preference: Preference) {

  def start(implicit preference: Preference): Unit = {
    
  }

}

