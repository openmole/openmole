package org.openmole.gui.plugin.environment.egi

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data.{ Error, ErrorBuilder }
import org.openmole.plugin.environment.egi.{ EGIAuthentication, P12Certificate }
import org.openmole.plugin.environment.ssh.{ LoginPassword, PrivateKey, SSHAuthentication }

import scala.util.{ Failure, Success, Try }

/**
 * Created by mathieu on 13/01/17.
 */
object AuthenticationFactories {

  implicit def dataToFactory(data: AuthenticationData): AuthenticationFactory = data match {
    case e: EGIAuthenticationData           ⇒ EGIP12Factory
    case l: LoginPasswordAuthenticationData ⇒ SSHLoginPasswordFactory
    case _                                  ⇒ SSHPrivateKeyFactory

  }

  def addAuthentication(data: AuthenticationData) = data.buildAuthentication(data)

  def allAuthentications(data: AuthenticationData) = data.allAuthenticationData

  def allAuthentications = EGIP12Factory.allAuthenticationData ++ SSHLoginPasswordFactory.allAuthenticationData ++ SSHPrivateKeyFactory.allAuthenticationData

  def removeAuthentication(data: AuthenticationData) = data.removeAuthentication(data)

  implicit def toAuthenticationTest[T](t: Try[T]): AuthenticationTest = t match {
    case Success(_) ⇒ AuthenticationTestBase(true, Error.empty)
    case Failure(f) ⇒ AuthenticationTestBase(false, ErrorBuilder(f))
  }

  trait AuthenticationFactory {
    def coreObject(data: AuthenticationData): Option[Any]

    def buildAuthentication(data: AuthenticationData): Unit

    def allAuthenticationData: Seq[AuthenticationData]

    def removeAuthentication(data: AuthenticationData): Unit
  }

  object EGIP12Factory extends AuthenticationFactory {
    implicit def workpsace = Workspace.instance

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.foreach { a ⇒
        val auth = coreObject(data)
        auth.foreach { a ⇒
          EGIAuthentication.update(a, test = false)
        }
      }
    }

    def allAuthenticationData: Seq[EGIAuthenticationData] = {
      println("EGI AUth " + EGIAuthentication())
      EGIAuthentication() match {
        case Some(p12: P12Certificate) ⇒
          Seq(EGIAuthenticationData(
            Workspace.decrypt(p12.cypheredPassword),
            Some(p12.certificate.getName)
          ))
        case x: Any ⇒ Seq()
      }
    }

    def coreObject(data: AuthenticationData): Option[P12Certificate] = data match {
      case p12: EGIAuthenticationData ⇒
        p12.privateKey match {
          case Some(pk: String) ⇒ Some(P12Certificate(
            Workspace.encrypt(p12.cypheredPassword),
            Utils.authenticationFile(pk)
          ))
          case _ ⇒ None
        }
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = EGIAuthentication.clear

    def testAuthentication(data: EGIAuthenticationData, voName: String): AuthenticationTest = coreObject(data).map { auth ⇒
      Try {
        EGIAuthenticationTest(
          voName,
          testPassword(data),
          EGIAuthentication.testProxy(auth, voName),
          EGIAuthentication.testDIRACAccess(auth, voName)
        )
      } match {
        case Success(a) ⇒ a
        case Failure(f) ⇒ EGIAuthenticationTest("Error", AuthenticationTest.empty, AuthenticationTestBase(false, ErrorBuilder(f)), AuthenticationTest.empty)
      }
    }.getOrElse(AuthenticationTestBase(false, ErrorBuilder(new Throwable("Unknown " + data.name))))

    def testPassword(data: AuthenticationData): AuthenticationTest = coreObject(data).map { d ⇒
      toAuthenticationTest(EGIAuthentication.testPassword(d))
    }.getOrElse(AuthenticationTestBase(false, Error("Unknown " + data.name)))

  }

  object SSHLoginPasswordFactory extends AuthenticationFactory {

    implicit def workspace = Workspace.instance

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.map { a ⇒ SSHAuthentication += a }
    }

    def allAuthenticationData: Seq[AuthenticationData] = SSHAuthentication().flatMap {
      _ match {
        case lp: LoginPassword ⇒ Some(LoginPasswordAuthenticationData(
          lp.login,
          Workspace.decrypt(lp.cypheredPassword),
          lp.host,
          lp.port.toString
        ))
        case _ ⇒ None
      }
    }

    def coreObject(data: AuthenticationData): Option[LoginPassword] = data match {
      case lp: LoginPasswordAuthenticationData ⇒ Some(LoginPassword(
        lp.login,
        Workspace.encrypt(lp.cypheredPassword),
        lp.target
      ))
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = coreObject(data).map { e ⇒
      SSHAuthentication -= e
    }

    def testAuthentication(data: LoginPasswordAuthenticationData): Seq[AuthenticationTest] = Seq(coreObject(data).map { auth ⇒
      SSHAuthentication.test(auth) match {
        case Success(_) ⇒ SSHAuthenticationTest(true, Error.empty)
        case Failure(f) ⇒ SSHAuthenticationTest(false, ErrorBuilder(f))
      }
    }.getOrElse(SSHAuthenticationTest(false, ErrorBuilder(new Throwable("Unknown " + data.name)))))
  }

  object SSHPrivateKeyFactory extends AuthenticationFactory {
    implicit def workspace = Workspace.instance

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.map { a ⇒ SSHAuthentication += a }
    }

    def allAuthenticationData: Seq[AuthenticationData] = SSHAuthentication().flatMap {
      _ match {
        case key: PrivateKey ⇒ Some(PrivateKeyAuthenticationData(
          Some(key.privateKey.getName),
          key.login,
          Workspace.decrypt(key.cypheredPassword),
          key.host,
          key.port.toString
        ))

        case _ ⇒ None
      }
    }

    def coreObject(data: AuthenticationData): Option[PrivateKey] = data match {
      case keyData: PrivateKeyAuthenticationData ⇒
        keyData.privateKey match {
          case Some(pk: String) ⇒ Some(PrivateKey(
            Utils.authenticationFile(pk),
            keyData.login,
            Workspace.encrypt(keyData.cypheredPassword),
            keyData.target
          ))
          case _ ⇒ None
        }
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = coreObject(data).map { e ⇒ SSHAuthentication -= e }

    def testAuthentication(data: PrivateKeyAuthenticationData): Seq[AuthenticationTest] = Seq(coreObject(data).map { auth ⇒
      SSHAuthentication.test(auth) match {
        case Success(_) ⇒ SSHAuthenticationTest(true, Error.empty)
        case Failure(f) ⇒ SSHAuthenticationTest(false, ErrorBuilder(f))
      }
    }.getOrElse(SSHAuthenticationTest(false, ErrorBuilder(new Throwable("Unknown " + data.name)))))

  }

  def testEGIAuthentication(data: EGIAuthenticationData, vos: Seq[String]): Seq[AuthenticationTest] =
    vos.map { vo ⇒ EGIP12Factory.testAuthentication(data, vo) }

  def testLoginPasswordSSHAuthentication(data: LoginPasswordAuthenticationData): Seq[AuthenticationTest] =
    SSHLoginPasswordFactory.testAuthentication(data)

  def testPrivateKeySSHAuthentication(data: PrivateKeyAuthenticationData): Seq[AuthenticationTest] =
    SSHPrivateKeyFactory.testAuthentication(data)

}
