package org.openmole.web.misc.tools

import java.security.KeyStore
import org.openmole.core.workspace.Workspace
import resource._
import java.io.{ FileOutputStream, FileInputStream, File }

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 12/8/13
 * Time: 11:27 PM
 */
object KeyStoreTools {
  def getOMInsecureKeyStore = getKeyStore(Workspace.file("OMUnsafeKeystore"), "".toCharArray)

  def getOMSecureKeyStore(pw: String) = getKeyStore(Workspace.file("OMKeyStore"), pw.toCharArray)

  def getKeyStore(ksLoc: File, pw: Array[Char]) = {
    val ks = KeyStore.getInstance(KeyStore.getDefaultType)

    val fis = managed(new FileInputStream(ksLoc))
    val fos = managed(new FileOutputStream(ksLoc))

    if (ksLoc.exists())
      fis foreach (ks.load(_, pw))
    else {
      ks.load(null, pw)
      fos foreach (ks.store(_, pw))
    }

    ks
  }

  //implement this with shapeless' coproducts
}