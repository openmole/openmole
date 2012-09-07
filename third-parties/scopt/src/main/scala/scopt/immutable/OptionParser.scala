package scopt.immutable

import scopt.generic._
import GenericOptionParser._

/**
 * <code>scopt.immutable.OptionParser</code> is instantiated within your object,
 * set up by implementing
 * <a href="options:Seq[OptionDefinition[C]]"><code>options</code></a> method that returns a sequence of invocations of
 * the various builder methods such as
 * <a href="#opt(String,String,String)((String, C) ⇒ C):ArgOptionDefinition[C]"><code>opt</code></a> method or
 * <a href="#arg(String,String)((String, C) ⇒ C):Argument[C]"><code>arg</code></a> method.
 * {{{
 * val parser = new scopt.immutable.OptionParser[Config]("scopt", "2.x") { def options = Seq(
 *   intOpt("f", "foo", "foo is an integer property") { (v: Int, c: Config) => c.copy(foo = v) },
 *   opt("o", "output", "output") { (v: String, c: Config) => c.copy(bar = v) },
 *   booleanOpt("xyz", "xyz is a boolean property") { (v: Boolean, c: Config) => c.copy(xyz = v) },
 *   keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>")
 *     { (key: String, value: String, c: Config) => c.copy(libname = key, libfile = value) },
 *   keyIntValueOpt(None, "max", "<libname>", "<max>", "maximum count for <libname>")
 *     { (key: String, value: Int, c: Config) => c.copy(maxlibname = key, maxcount = value) },
 *   arg("<file>", "some argument") { (v: String, c: Config) => c.copy(whatnot = v) }
 * ) }
 * // parser.parse returns Option[C]
 * parser.parse(args, Config()) map { config =>
 *   // do stuff
 * } getOrElse {
 *   // arguments are bad, usage message will have been displayed
 * }
 * }}}
 */
abstract case class OptionParser[C](
    programName: Option[String] = None,
    version: Option[String] = None,
    errorOnUnknownArgument: Boolean = true) extends GenericOptionParser[C] {

  def this() = this(None, None, true)
  def this(programName: String) = this(Some(programName), None, true)
  def this(programName: String, version: String) = this(Some(programName), Some(version), true)
  def this(errorOnUnknownArgument: Boolean) = this(None, None, errorOnUnknownArgument)
  def this(programName: String, errorOnUnknownArgument: Boolean) =
    this(Some(programName), None, errorOnUnknownArgument)

  /**
   * adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(shortopt: String, longopt: String, description: String)(action: (String, C) ⇒ C) =
    new ArgOptionDefinition[C](Some(shortopt), longopt, defaultValueName, description, action)

  /**
   * adds a `String` option invoked by `--longopt x`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(longopt: String, description: String)(action: (String, C) ⇒ C) =
    new ArgOptionDefinition[C](None, longopt, defaultValueName, description, action)

  /**
   * adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(shortopt: String, longopt: String, valueName: String,
          description: String)(action: (String, C) ⇒ C) =
    new ArgOptionDefinition[C](Some(shortopt), longopt, valueName, description, action)

  /**
   * adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option, or `None`
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(shortopt: Option[String], longopt: String, valueName: String,
          description: String)(action: (String, C) ⇒ C) =
    new ArgOptionDefinition[C](shortopt, longopt, valueName, description, action)

  /**
   * adds a flag option invoked by `-shortopt` or `--longopt`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def flag(shortopt: String, longopt: String, description: String)(action: C ⇒ C) =
    new FlagOptionDefinition[C](Some(shortopt), longopt, description, action)

  /**
   * adds a flag option invoked by `--longopt`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def flag(longopt: String, description: String)(action: C ⇒ C) =
    new FlagOptionDefinition[C](None, longopt, description, action)

  // we have to give these typed options separate names, because of &^@$! type erasure
  def intOpt(shortopt: String, longopt: String, description: String)(action: (Int, C) ⇒ C) =
    new IntArgOptionDefinition[C](Some(shortopt), longopt, defaultValueName, description, action)

  def intOpt(longopt: String, description: String)(action: (Int, C) ⇒ C) =
    new IntArgOptionDefinition[C](None, longopt, defaultValueName, description, action)

  def intOpt(shortopt: String, longopt: String, valueName: String,
             description: String)(action: (Int, C) ⇒ C) =
    new IntArgOptionDefinition[C](Some(shortopt), longopt, valueName, description, action)

  def intOpt(shortopt: Option[String], longopt: String, valueName: String,
             description: String)(action: (Int, C) ⇒ C) =
    new IntArgOptionDefinition[C](shortopt, longopt, valueName, description, action)

  def doubleOpt(shortopt: String, longopt: String, description: String)(action: (Double, C) ⇒ C) =
    new DoubleArgOptionDefinition[C](Some(shortopt), longopt, defaultValueName, description, action)

  def doubleOpt(longopt: String, description: String)(action: (Double, C) ⇒ C) =
    new DoubleArgOptionDefinition[C](None, longopt, defaultValueName, description, action)

  def doubleOpt(shortopt: String, longopt: String, valueName: String,
                description: String)(action: (Double, C) ⇒ C) =
    new DoubleArgOptionDefinition[C](Some(shortopt), longopt, valueName, description, action)

  def doubleOpt(shortopt: Option[String], longopt: String, valueName: String,
                description: String)(action: (Double, C) ⇒ C) =
    new DoubleArgOptionDefinition[C](shortopt, longopt, valueName, description, action)

  def booleanOpt(shortopt: String, longopt: String, description: String)(action: (Boolean, C) ⇒ C) =
    new BooleanArgOptionDefinition[C](Some(shortopt), longopt, defaultValueName, description, action)

  def booleanOpt(longopt: String, description: String)(action: (Boolean, C) ⇒ C) =
    new BooleanArgOptionDefinition[C](None, longopt, defaultValueName, description, action)

  def booleanOpt(shortopt: String, longopt: String, valueName: String,
                 description: String)(action: (Boolean, C) ⇒ C) =
    new BooleanArgOptionDefinition[C](Some(shortopt), longopt, valueName, description, action)

  def booleanOpt(shortopt: Option[String], longopt: String, valueName: String,
                 description: String)(action: (Boolean, C) ⇒ C) =
    new BooleanArgOptionDefinition[C](shortopt, longopt, valueName, description, action)

  def keyValueOpt(shortopt: String, longopt: String, description: String)(action: (String, String, C) ⇒ C) =
    new KeyValueArgOptionDefinition[C](Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyValueOpt(longopt: String, description: String)(action: (String, String, C) ⇒ C) =
    new KeyValueArgOptionDefinition[C](None, longopt, defaultKeyName, defaultValueName, description, action)

  def keyValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
                  description: String)(action: (String, String, C) ⇒ C) =
    new KeyValueArgOptionDefinition[C](Some(shortopt), longopt, keyName, valueName, description, action)

  def keyValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
                  description: String)(action: (String, String, C) ⇒ C) =
    new KeyValueArgOptionDefinition[C](shortopt, longopt, keyName, valueName, description, action)

  def keyIntValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Int, C) ⇒ C) =
    new KeyIntValueArgOptionDefinition[C](Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyIntValueOpt(longopt: String, description: String)(action: (String, Int, C) ⇒ C) =
    new KeyIntValueArgOptionDefinition[C](None, longopt, defaultKeyName, defaultValueName, description, action)

  def keyIntValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
                     description: String)(action: (String, Int, C) ⇒ C) =
    new KeyIntValueArgOptionDefinition[C](Some(shortopt), longopt, keyName, valueName, description, action)

  def keyIntValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
                     description: String)(action: (String, Int, C) ⇒ C) =
    new KeyIntValueArgOptionDefinition[C](shortopt, longopt, keyName, valueName, description, action)

  def keyDoubleValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Double, C) ⇒ C) =
    new KeyDoubleValueArgOptionDefinition[C](Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyDoubleValueOpt(longopt: String, description: String)(action: (String, Double, C) ⇒ C) =
    new KeyDoubleValueArgOptionDefinition[C](None, longopt, defaultKeyName, defaultValueName, description, action)

  def keyDoubleValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
                        description: String)(action: (String, Double, C) ⇒ C) =
    new KeyDoubleValueArgOptionDefinition[C](Some(shortopt), longopt, keyName, valueName, description, action)

  def keyDoubleValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
                        description: String)(action: (String, Double, C) ⇒ C) =
    new KeyDoubleValueArgOptionDefinition[C](shortopt, longopt, keyName, valueName, description, action)

  def keyBooleanValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Boolean, C) ⇒ C) =
    new KeyBooleanValueArgOptionDefinition[C](Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyBooleanValueOpt(longopt: String, description: String)(action: (String, Boolean, C) ⇒ C) =
    new KeyBooleanValueArgOptionDefinition[C](None, longopt, defaultKeyName, defaultValueName, description, action)

  def keyBooleanValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
                         description: String)(action: (String, Boolean, C) ⇒ C) =
    new KeyBooleanValueArgOptionDefinition[C](Some(shortopt), longopt, keyName, valueName, description, action)

  def keyBooleanValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
                         description: String)(action: (String, Boolean, C) ⇒ C) =
    new KeyBooleanValueArgOptionDefinition[C](shortopt, longopt, keyName, valueName, description, action)

  def help(shortopt: String, longopt: String, description: String) =
    new FlagOptionDefinition[C](Some(shortopt), longopt, description, { _ ⇒ this.showUsage; exit })

  def help(shortopt: Option[String], longopt: String, description: String) =
    new FlagOptionDefinition[C](shortopt, longopt, description, { _ ⇒ this.showUsage; exit })

  def separator(description: String) =
    new SeparatorDefinition[C](description)

  /**
   * adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arg(name: String, description: String)(action: (String, C) ⇒ C) =
    new Argument[C](name, description, 1, 1, action)

  /**
   * adds an optional argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def argOpt(name: String, description: String)(action: (String, C) ⇒ C) =
    new Argument[C](name, description, 0, 1, action)

  /**
   * adds a list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglist(name: String, description: String)(action: (String, C) ⇒ C) =
    new Argument[C](name, description, 1, UNBOUNDED, action)

  /**
   * adds an optional list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglistOpt(name: String, description: String)(action: (String, C) ⇒ C) =
    new Argument[C](name, description, 0, UNBOUNDED, action)

}
