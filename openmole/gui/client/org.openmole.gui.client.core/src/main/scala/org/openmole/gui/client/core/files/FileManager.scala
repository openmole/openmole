package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.Utils
import org.scalajs.dom.raw._

/*
 * Copyright (C) 29/04/15 // mathieu.leclaire@openmole.org
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

object FileManager {

  def fileNames(fileList: FileList): Seq[String] = {
    var nameList = Seq[String]()
    for (i ← 0 to fileList.length - 1) {
      nameList = nameList :+ fileList(i).name
    }
    nameList
  }

  def upload(
    inputElement:      HTMLInputElement,
    destinationPath:   SafePath,
    fileTransferState: ProcessState ⇒ Unit,
    uploadType:        UploadType,
    onloaded:          () ⇒ Unit           = () ⇒ {}
  ) = {
    val fileList = inputElement.files
    val formData = new FormData

    formData.append("fileType", uploadType.typeName)

    for (i ← 0 to fileList.length - 1) {
      val file = fileList(i)
      formData.append(Utils.toURI(destinationPath.path ++ Seq(file.name)), file)
    }

    val xhr = new XMLHttpRequest

    xhr.upload.onprogress = (e: ProgressEvent) ⇒ {
      fileTransferState(Processing((e.loaded.toDouble * 100 / e.total).toInt))
    }

    xhr.upload.onloadend = (e: ProgressEvent) ⇒ {
      fileTransferState(Finalizing())
    }

    xhr.onloadend = (e: ProgressEvent) ⇒ {
      fileTransferState(Processed())
      onloaded()
      inputElement.value = ""
    }

    xhr.open("POST", "uploadfiles", true)
    xhr.send(formData)
  }

  def download(
    treeNode:          TreeNode,
    fileTransferState: ProcessState ⇒ Unit,
    onLoadEnded:       String ⇒ Unit
  ) = {

    val xhr = new XMLHttpRequest

    xhr.onprogress = (e: ProgressEvent) ⇒ {
      fileTransferState(Processing((e.loaded.toDouble * 100 / treeNode.size).toInt))
    }

    xhr.onloadend = (e: ProgressEvent) ⇒ {
      fileTransferState(Processed())
      if (treeNode.safePath().extension.displayable) {
        onLoadEnded(xhr.responseText)
      }
    }

    xhr.open("GET", s"downloadFile?path=${Utils.toURI(treeNode.safePath().path)}", true)
    xhr.send()
  }

}

