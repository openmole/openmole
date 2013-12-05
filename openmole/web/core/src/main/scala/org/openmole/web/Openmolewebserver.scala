package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.ScalatraBase
import java.security.{ Security, SecureRandom, KeyPairGenerator, KeyStore }
import java.io.{ FileOutputStream, FileInputStream, File }
import resource._
import org.bouncycastle.x509.X509V3CertificateGenerator
import org.bouncycastle.asn1.x509.X509Name
import java.math.BigInteger
import java.util.Date
import org.bouncycastle.jce.X509Principal
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider.Service
import org.openmole.misc.workspace.Workspace

class Openmolewebserver(port: Option[Int], sslPort: Option[Int], hostName: Option[String], pass: Option[String]) {

  val p = port getOrElse 8080
  val sslP = sslPort getOrElse 8443

  println(s"binding http to: $p")

  val server = new Server(p)

  val contextFactory = new org.eclipse.jetty.http.ssl.SslContextFactory()

  val ks = KeyStore.getInstance(KeyStore.getDefaultType)

  val bcp = new BouncyCastleProvider

  val pw = pass getOrElse "openmole"
  val host = hostName getOrElse "localhost"

  val ksLoc = Workspace.file("OMServerKeystore")

  // ~/.openmole/keystore
  val fis = managed(new FileInputStream(ksLoc))
  val fos = managed(new FileOutputStream(ksLoc))
  //for ((s: Service) ← services) println(s.asInstanceOf[Service].getAlgorithm)

  if (ksLoc.exists())
    fis foreach (ks.load(_, pw.toCharArray))
  else {
    Security.addProvider(bcp)

    ks.load(null, pw.toCharArray)

    val certGen = new X509V3CertificateGenerator()

    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024, new SecureRandom())
    val kp = kpg.generateKeyPair()

    certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
    certGen.setIssuerDN(new X509Principal("CN=cn, O=o, L=L, ST=il, C= c"))
    certGen.setSubjectDN(new X509Principal("CN=cn, O=o, L=L, ST=il, C= c"))
    certGen.setNotBefore(new Date(System.currentTimeMillis() - 1000l * 60 * 60 * 24))
    certGen.setNotAfter(new Date(System.currentTimeMillis() + 1000l * 60 * 60 * 24 * 365 * 1000))
    certGen.setPublicKey(kp.getPublic)
    certGen.setSignatureAlgorithm("SHA256WITHRSA")
    val cert = certGen.generate(kp.getPrivate)

    ks.setKeyEntry(host, kp.getPrivate, pw.toCharArray, Array[java.security.cert.Certificate](cert))

    fos foreach (ks.store(_, pw.toCharArray))
  }

  contextFactory.setKeyStore(ks)
  contextFactory.setKeyStorePassword(pw)
  contextFactory.setKeyManagerPassword(pw)
  contextFactory.setTrustStore(ks)
  contextFactory.setTrustStorePassword(pw)

  println(s"binding ssl to: $sslP")

  server.addConnector(new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory) {
    setPort(sslP)
    setMaxIdleTime(30000)
  })

  val context = new WebAppContext()

  val res = Res.newResource(classOf[Openmolewebserver].getResource("/"))

  context.setContextPath("/")
  context.setBaseResource(res)
  context.setClassLoader(classOf[Openmolewebserver].getClassLoader)
  context.setInitParameter(ScalatraBase.HostNameKey, host)
  context.setInitParameter("org.scalatra.Port", sslP.toString)
  context.setInitParameter(ScalatraBase.ForceHttpsKey, "true")

  server.setHandler(context)

  def start() {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
  }
}