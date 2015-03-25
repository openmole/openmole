package org.openmole.web

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.operator.{ DefaultDigestAlgorithmIdentifierFinder, DefaultSignatureAlgorithmIdentifierFinder }
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.scalatra.ScalatraBase
import java.security.{ Security, SecureRandom, KeyPairGenerator, KeyStore }
import java.io.{ FileOutputStream, FileInputStream }
import org.scalatra.servlet.ScalatraListener
import resource._
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.asn1.x509._
import java.math.BigInteger
import java.util.Date
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.jetty.security.{ ConstraintMapping, ConstraintSecurityHandler }
import org.openmole.web.db.SlickDB
import org.openmole.core.tools.io.HashUtil._

object RESTServer extends Logger {
  def passwordHash = ConfigurationLocation("REST", "PasswordHash", true)
  def setPassword(p: String) = Workspace.setPreference(passwordHash, p.hash.toString)
  def isPasswordCorrect(p: String) = Workspace.preference(passwordHash) == p.hash.toString
}

import RESTServer.Log._

class RESTServer(port: Option[Int], sslPort: Option[Int], hostName: Option[String], pass: Option[String], allowInsecureConnections: Boolean) {

  val p = port getOrElse 8080
  val sslP = sslPort getOrElse 8443

  val server =
    if (allowInsecureConnections) {
      logger.info(s"Binding http to port $p")
      new Server(p)
    }
    else new Server()

  val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()

  //replace with keystore utils' getOMSecureKeyStore
  val ks = KeyStore.getInstance(KeyStore.getDefaultType)

  val bcp = new BouncyCastleProvider()
  Security.addProvider(bcp)

  val ksLoc = Workspace.file("OMServerKeystore")

  val ksPassword = ""
  val dbPassword = ""

  if (ksLoc.exists()) {
    val fis = managed(new FileInputStream(ksLoc))
    fis foreach (ks.load(_, ksPassword.toCharArray))
  }
  else {
    ks.load(null, "".toCharArray)

    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024, new SecureRandom())
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

    val sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA")
    val digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val privateKeyInfo = PrivateKeyInfo.getInstance(kp.getPrivate.getEncoded)
    val signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(privateKeyInfo))
    val holder = certificateBuilder.build(signer)

    val cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
    ks.setKeyEntry(hostName getOrElse "", kp.getPrivate, ksPassword.toCharArray, Array[java.security.cert.Certificate](cert))

    val fos = managed(new FileOutputStream(ksLoc))
    fos foreach (ks.store(_, ksPassword.toCharArray))
  }

  contextFactory.setKeyStore(ks)
  contextFactory.setKeyStorePassword(ksPassword)
  contextFactory.setKeyManagerPassword(ksPassword)
  contextFactory.setTrustStore(ks)
  contextFactory.setTrustStorePassword(ksPassword)

  logger.info(s"binding https to port $sslP")

  server.addConnector(
    new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory) {
      setPort(sslP)
      setMaxIdleTime(30000)
    }
  )

  val context = new WebAppContext()

  val res = Res.newResource(classOf[RESTServer].getClassLoader.getResource("/"))

  context.setContextPath("/")
  context.setBaseResource(res)
  context.setClassLoader(classOf[RESTServer].getClassLoader)
  hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
  context.setInitParameter("org.scalatra.Port", sslP.toString)
  context.setInitParameter(ScalatraBase.ForceHttpsKey, allowInsecureConnections.toString)

  //TODO: Discuss the protection of in-memory data for a java program.
  val db = new SlickDB(dbPassword)

  context.setAttribute("database", db)

  val constraintHandler = new ConstraintSecurityHandler
  val constraintMapping = new ConstraintMapping
  constraintMapping.setPathSpec("/*")
  constraintMapping.setConstraint({ val r = new org.eclipse.jetty.util.security.Constraint(); r.setDataConstraint(1); r })
  constraintHandler.addConstraintMapping(constraintMapping)

  if (!allowInsecureConnections) context.setSecurityHandler(constraintHandler)

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