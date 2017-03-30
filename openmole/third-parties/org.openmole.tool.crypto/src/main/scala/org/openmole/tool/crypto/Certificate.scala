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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.tool.crypto

import java.io.{ File, FileInputStream, FileOutputStream }
import java.math.BigInteger
import java.security.{ KeyPairGenerator, SecureRandom }
import java.util.Date

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.{ Extension, GeneralName, GeneralNames, SubjectPublicKeyInfo }
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.operator.{ DefaultDigestAlgorithmIdentifierFinder, DefaultSignatureAlgorithmIdentifierFinder }
import org.bouncycastle.jce.provider.BouncyCastleProvider

object Certificate {

  //Security.addProvider(new BouncyCastleProvider())

  def loadOrGenerate(file: File, ksPassword: String, hostName: Option[String] = Some("OpenMOLE")) = {
    val ks = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType)
    if (file.exists()) {
      val fis = new FileInputStream(file)
      try ks.load(fis, ksPassword.toCharArray)
      finally fis.close
    }
    else {
      ks.load(null, "".toCharArray)

      val kpg = KeyPairGenerator.getInstance("RSA")
      kpg.initialize(2048, new SecureRandom())
      val kp = kpg.generateKeyPair()

      val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
      val issuerDN = new X500Name(s"CN=${hostName getOrElse "cn"}, O=o, L=L, ST=il, C= c")
      val subjectDN = new X500Name(s"CN=${hostName getOrElse "cn"}, O=o, L=L, ST=il, C= c")
      val noBefore = new Date(System.currentTimeMillis() - 1000l * 60 * 60 * 24)
      val noAfter = new Date(System.currentTimeMillis() + 1000l * 60 * 60 * 24 * 365 * 1000)
      val subjectPublicInfo = SubjectPublicKeyInfo.getInstance(kp.getPublic.getEncoded)

      val certificateBuilder = new X509v3CertificateBuilder(issuerDN, serialNumber, noBefore, noAfter, subjectDN, subjectPublicInfo)

      val altNameExtension = Extension.subjectAlternativeName
      val subjectAltName = new GeneralNames(new GeneralName(GeneralName.rfc822Name, "127.0.0.1"))
      certificateBuilder.addExtension(altNameExtension, false, subjectAltName)

      val sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA")
      val digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
      val privateKeyInfo = PrivateKeyInfo.getInstance(kp.getPrivate.getEncoded)
      val signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(privateKeyInfo))
      val holder = certificateBuilder.build(signer)

      val cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(holder)
      ks.setKeyEntry(hostName getOrElse "", kp.getPrivate, ksPassword.toCharArray, Array[java.security.cert.Certificate](cert))

      val fos = new FileOutputStream(file)
      try ks.store(fos, ksPassword.toCharArray)
      finally fos.close
    }
    ks
  }
}
