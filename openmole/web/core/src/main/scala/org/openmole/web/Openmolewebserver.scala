package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.core.workspace.Workspace
import org.scalatra.ScalatraBase
import java.security.{ Security, SecureRandom, KeyPairGenerator, KeyStore }
import java.io.{ FileOutputStream, FileInputStream, File }
import resource._
import org.bouncycastle.x509.X509V3CertificateGenerator
import org.bouncycastle.asn1.x509._
import java.math.BigInteger
import java.util.Date
import org.bouncycastle.jce.X509Principal
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider.Service
import org.bouncycastle.asn1.{ ASN1EncodableVector, DEROctetString, ASN1ObjectIdentifier, DERObjectIdentifier }
import sun.security.x509.SubjectAlternativeNameExtension
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.security.{ ConstraintMapping, ConstraintSecurityHandler }
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import org.openmole.web.db.SlickDB;

class Openmolewebserver(port: Option[Int], sslPort: Option[Int], hostName: Option[String], pass: Option[String], allowInsecureConnections: Boolean) {

  val p = port getOrElse 8080
  val sslP = sslPort getOrElse (if (port.isDefined) 8443 else 8080)

  val server = if (allowInsecureConnections) {
    println(s"Binding http to port $p")
    new Server(p)
  }
  else new Server()

  val contextFactory = new org.eclipse.jetty.http.ssl.SslContextFactory()

  //replace with keystore utils' getOMSecureKeyStore
  val ks = KeyStore.getInstance(KeyStore.getDefaultType)

  val bcp = new BouncyCastleProvider()

  val pw = pass getOrElse "openmole"
  //val host = hostName getOrElse "localhost"

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
    certGen.setIssuerDN(new X509Principal(s"CN=${hostName getOrElse "cn"}, O=o, L=L, ST=il, C= c"))
    certGen.setSubjectDN(new X509Principal(s"CN=${hostName getOrElse "cn"}, O=o, L=L, ST=il, C= c"))
    val subjectAltName = new GeneralNames(
      new GeneralName(GeneralName.rfc822Name, "127.0.0.1"))

    val oids = new java.util.Vector[ASN1ObjectIdentifier]
    val vals = new java.util.Vector[X509Extension]
    oids.add(X509Extensions.SubjectAlternativeName)
    vals.add(new X509Extension(false, new DEROctetString(subjectAltName)))
    val extensions = new X509Extensions(oids, vals)
    certGen.setNotBefore(new Date(System.currentTimeMillis() - 1000l * 60 * 60 * 24))
    certGen.setNotAfter(new Date(System.currentTimeMillis() + 1000l * 60 * 60 * 24 * 365 * 1000))
    certGen.setPublicKey(kp.getPublic)
    certGen.setSignatureAlgorithm("SHA256WITHRSA")
    certGen.addExtension("2.5.29.17", false, "hell".toCharArray.map(_.toByte)) //Subject alt name oid?
    val cert = certGen.generate(kp.getPrivate)

    ks.setKeyEntry(hostName getOrElse "", kp.getPrivate, pw.toCharArray, Array[java.security.cert.Certificate](cert))

    fos foreach (ks.store(_, pw.toCharArray))
  }

  contextFactory.setKeyStore(ks)
  contextFactory.setKeyStorePassword(pw)
  contextFactory.setKeyManagerPassword(pw)
  contextFactory.setTrustStore(ks)
  contextFactory.setTrustStorePassword(pw)

  println(s"binding https to port $sslP")

  server.addConnector(new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory) {
    setPort(sslP)
    setMaxIdleTime(30000)
  })

  /*if (!allowInsecureConnections)
    server.getConnectors.foreach {
      case s: SelectChannelConnector ⇒ server.removeConnector(s)
      case _                         ⇒ ()
    }*/

  val context = new WebAppContext()

  val res = Res.newResource(classOf[Openmolewebserver].getResource("/"))

  context.setContextPath("/")
  context.setBaseResource(res)
  context.setClassLoader(classOf[Openmolewebserver].getClassLoader)
  hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
  context.setInitParameter("org.scalatra.Port", sslP.toString)
  context.setInitParameter(ScalatraBase.ForceHttpsKey, allowInsecureConnections.toString)

  //TODO: Discuss the protection of in-memory data for a java program.
  val db = new SlickDB(pw)

  context.setAttribute("database", db)

  val constraintHandler = new ConstraintSecurityHandler
  val constraintMapping = new ConstraintMapping
  constraintMapping.setPathSpec("/*")
  constraintMapping.setConstraint({ val r = new org.eclipse.jetty.util.security.Constraint(); r.setDataConstraint(1); r })
  constraintHandler.addConstraintMapping(constraintMapping)

  if (!allowInsecureConnections) context.setSecurityHandler(constraintHandler)
  //context.setInitParameter("org.scalatra.environment", "production")

  server.setHandler(context)

  def start() {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
    db.closeDbConnection()
  }
}