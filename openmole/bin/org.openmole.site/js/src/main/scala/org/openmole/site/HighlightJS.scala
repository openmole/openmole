package org.openmole.site

import org.querki.jsext
import org.scalajs.dom.Element

import jsext._
import scala.scalajs.js
import scala.scalajs.js.annotation._

/**
  * @see [[https://highlightjs.org/usage/]]
  */



@js.native
@JSImport("highlight.js", JSImport.Namespace)
object HighlightJS extends HighlightStatic

@js.native
@JSImport("highlight.js/lib/languages/scala.js", JSImport.Namespace)
object scalamode extends js.Object

@js.native
trait HighlightStatic extends js.Object {
  /**
    * Core highlighting function. Accepts a language name, or an alias, and a string with the code to highlight. The ignore_illegals parameter, when present and evaluates to a true value, forces highlighting to finish even in case of detecting illegal syntax for the language instead of throwing an exception. The continuation is an optional mode stack representing unfinished parsing. When present, the function will restart parsing from this state instead of initializing a new one.
    *
    * @return Returns an object with the following properties:
    *         language: language name, same as the one passed into a function, returned for consistency with highlightAuto
    *         relevance: integer value
    *         value: HTML string with highlighting markup
    *         top: top of the current mode stack
    */
  def highlight(name: String, value: String, ignoreIllegals: js.UndefOr[Boolean] = js.native, continuation: js.UndefOr[js.Object] = js.native): HighlightJSResult = js.native

  /**
    * Highlighting with language detection. Accepts a string with the code to highlight and an optional array of language names and aliases restricting detection to only those languages. The subset can also be set with configure, but the local parameter overrides the option if set.
    *
    * @return Returns an object with the following properties:
    *         language: detected language
    *         relevance: integer value
    *         value: HTML string with highlighting markup
    *         second_best: object with the same structure for second-best heuristically detected language, may be absent
    */
  def highlightAuto(value: String, languageSubset: js.UndefOr[js.Array[String]] = js.native): HighlightJSResult = js.native


  /**
    * Applies highlighting to all `<pre><code>..</code></pre>` blocks on a page.
    */
  def initHighlighting(): Unit = js.native

  /**
    * Attaches highlighting to the page load event.
    */
  def initHighlightingOnLoad(): Unit = js.native

  /**
    * Applies highlighting to a DOM node containing code.
    * This function is the one to use to apply highlighting dynamically after page load or within initialization code of third-party Javascript frameworks.
    * The function uses language detection by default but you can specify the language in the class attribute of the DOM node. See the class reference for all available language names and aliases.
    */
  def highlightBlock(el: Element):Unit = js.native

  /**
    * Post-processing of the highlighted markup. Currently consists of replacing indentation TAB characters and using <br> tags instead of new-line characters. Options are set globally with configure.
    * Accepts a string with the highlighted markup.
    */
  val fixMarkup: js.UndefOr[String] = js.native

  /**
    * Configures global options:
    * *
    * tabReplace: a string used to replace TAB characters in indentation.
    * useBR: a flag to generate <br> tags instead of new-line characters in the output, useful when code is marked up using a non-<pre> container.
    * classPrefix: a string prefix added before class names in the generated markup, used for backwards compatibility with stylesheets.
    * languages: an array of language names and aliases restricting auto detection to only these languages.
    * Accepts an object representing options with the values to updated. Other options don’t change
    * {{{
    *   hljs.configure({
    *     tabReplace: '    ', // 4 spaces
    *     classPrefix: ''     // don't append class prefix
    *                         // … other options aren't changed
    *   });
    *   hljs.initHighlighting();
    * }}}
    */
  val configure: js.UndefOr[js.Object] = js.native


  /**
    * Adds new language to the library under the specified name. Used mostly internally.
    *
    * @param name     A string with the name of the language being registered
    * @param language A function that returns an object which represents the language definition. The function is passed the hljs object to be able to use common regular expressions defined within it.
    */
  def registerLanguage(name: String, language: js.Function): Unit = js.native

  /**
    * Returns the languages names list.
    */
  def listLanguages(): js.Array[String] = js.native

  /**
    * Looks up a language by name or alias.
    * Returns the language object if found, `undefined` otherwise.
    */
  def getLanguage(name: String): js.UndefOr[js.Object] = js.native
}


object HighlightStatic extends HighlightStaticBuilder(noOpts)

class HighlightStaticBuilder(val dict: OptMap) extends JSOptionBuilder[HighlightStatic, HighlightStaticBuilder](new HighlightStaticBuilder(_)) {

  def fixMarkup(v: String) = jsOpt("fixMarkup", v)

  def configure(v: js.Object) = jsOpt("configure", v)


}

@js.native
trait HighlightJSResult extends js.Object {
  /**
    * Detected language
    */
  def language: String = js.native

  /**
    * Integer value
    */
  def relevance: Int = js.native

  /**
    * HTML string with highlighting markup
    */
  def value: String = js.native

  /**
    * Top of the current mode stack
    */
  def top: js.Object = js.native

//  /**
//    * Object with the same structure for second-best heuristically detected language, may be absent
//    */
//  @JSName("second_best")
//  def secondBest: js.Object = js.native
}
