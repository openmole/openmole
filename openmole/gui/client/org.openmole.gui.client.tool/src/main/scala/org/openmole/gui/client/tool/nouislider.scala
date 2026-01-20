package org.openmole.gui.client.tool

import org.querki.jsext._
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js
import scala.scalajs.js.|
import js.annotation._

object nouislider:

  @js.native
  trait NoUiSliderOptions extends js.Object {
    val range: js.UndefOr[Range] = js.native

    val start: js.UndefOr[Double | js.Array[Double]] = js.native

    val connect: js.UndefOr[Boolean | js.Array[Boolean]] = js.native

    val limit: js.UndefOr[Double] = js.native

    val step: js.UndefOr[Double] = js.native

    val orientation: js.UndefOr[Options.Orientation] = js.native

    val direction: js.UndefOr[Options.Direction] = js.native

    val tooltips: js.UndefOr[Boolean | js.Array[Boolean]] = js.native
  }


  object Options extends NoUiSliderOptionsBuilder(noOpts):
    type Connect = String
    val Upper: Connect = "upper"
    val Lower: Connect = "lower"

    type Orientation = String
    val Horizontal = "horizontal"
    val Vertical = "vertical"

    type Direction = String
    val RightToLeft = "rtl"
    val LeftToRgiht = "ltr"
    val BottomToTop = "btt"
    val TopToBottom = "ttb"


  class NoUiSliderOptionsBuilder(val dict: OptMap) extends JSOptionBuilder[NoUiSliderOptions, NoUiSliderOptionsBuilder](new NoUiSliderOptionsBuilder(_)) {
    def range(v: Range) = jsOpt("range", v)

    def start(v: Double | js.Array[Double]) = jsOpt("start", v)

    def connect(c: Options.Connect | js.Array[Boolean]) = jsOpt("connect", c)

    def limit(c: Double) = jsOpt("limit", c)

    def step(c: Double) = jsOpt("step", c)

    def orientation(o: Options.Orientation) = jsOpt("orientation", o)

    def direction(d: Options.Direction) = jsOpt("direction", d)

    def tooltips(t: Boolean | js.Array[Boolean]) = jsOpt("tooltips", t)

  }

  @js.native
  trait Range extends js.Object {
    def min: js.UndefOr[Double] = js.native

    def max: js.UndefOr[Double] = js.native
  }

  object Range extends RangeBuilder(noOpts)

  class RangeBuilder(val dict: OptMap) extends JSOptionBuilder[Range, RangeBuilder](new RangeBuilder(_)) {
    def min(v: Double) = jsOpt("min", v)

    def max(v: Double) = jsOpt("max", v)
  }

  object event:
    type SliderEventType = String
    val StartEvent: SliderEventType = "start"
    val SlideEvent: SliderEventType = "slide"
    val Dragevent: SliderEventType = "drag"
    val UpdateEvent: SliderEventType = "update"
    val ChangeEvent: SliderEventType = "change"
    val SetEvent: SliderEventType = "set"
    val EndEvent: SliderEventType = "end"

  @js.native
  @JSImport("nouislider", JSImport.Namespace)
  object noUiSlider extends js.Object {

    def create(element: org.scalajs.dom.HTMLElement, options: NoUiSliderOptions): Unit = js.native
  }

  object NoUISliderImplicits {
    implicit class NoUiSliderAPIDom(private val elem: org.scalajs.dom.HTMLElement) {
      def noUiSlider: NoUiSliderAPI = elem.asInstanceOf[js.Dynamic].noUiSlider.asInstanceOf[NoUiSliderAPI]
    }

    implicit class NoUiSlideAPILaminar(private val elem: HtmlElement) {
      def noUiSlider: NoUiSliderAPI = elem.ref.asInstanceOf[js.Dynamic].noUiSlider.asInstanceOf[NoUiSliderAPI]
    }
  }
  @js.native
  trait NoUiSliderAPI extends js.Object:
    def set(x: Double | js.Array[Double]): Unit = js.native

    def get(): String | js.Array[String] = js.native

    // values: Current slider values (array);
    // handle: Handle that caused the event (number);
    // unencoded: Slider values without formatting (array);
    // tap: Event was caused by the user tapping the slider (boolean);
    // positions: Left offset of the handles (array);
    // noUiSlider: slider public Api (noUiSlider);
    def on(event: org.openmole.gui.client.tool.nouislider.event.SliderEventType, callback: js.Function2[Double | js.Array[Double], Double, Unit]): Unit = js.native

