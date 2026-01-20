package org.openmole.gui.client.tool.bootstrapnative

/*
 * Copyright (C) 27/05/15 // mathieu.leclaire@openmole.org
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

//import net.scalapro.sortable.{EventS, Sortable, SortableOptions}

import org.openmole.gui.client.tool.randomId
import com.raquo.laminar.api.L.{*, given}
import org.openmole.gui.client.tool.bootstrapnative.bsn.spacing.*
import Popup.*
import Table.{DataContent, DataTableBuilder, ElementTableBuilder, Row}
import org.openmole.gui.client.tool

import scala.scalajs.js.|

object BootstrapTags extends BootstrapTags

trait BootstrapTags {
  bstags =>

  type HESetter = Setter[HtmlElement]

  type HESetters = Seq[HESetter]

  implicit def HESetterToHeSetters(setter: HESetter): HESetters = Seq(setter)

  val emptySetters: HESetters = Seq[HESetter]()

  def inputTag(content: String = "") = input(tool.bootstrapnative.bsn.formControl, value := content)

  //  implicit def inputGroupToDiv(inputGroup: InputGroup): HtmlElement =
  //    inputGroupBS.amend(
  //      inputGroupAppend(inputGroup.app)
  //        //.amend(inputGroupPrepend(inputGroup.pre))
  //        .amend(inputGroup.el)
  //
  //    )
  //
  //  case class InputGroup(pre: HtmlElement = div(), el: HtmlElement = div(), app: HtmlElement = div()) {
  //    def prepend(preElement: HtmlElement)= copy(pre = preElement)
  //
  //    def element(el: HtmlElement) = copy(el = el)
  //
  //    def append(appElement: HtmlElement) = copy(appElement)
  //  }
  //
  //  val inputGroup = InputGroup()

  def stickBefore(element: HtmlElement, withSticker: HtmlElement) =
    div(
      tool.bootstrapnative.bsnsheet.inputGroupClass,
      inputGroupPrepend(withSticker),
      element
    )

  def stickAfter(element: HtmlElement, withSticker: HtmlElement) =
    div(
      tool.bootstrapnative.bsnsheet.inputGroupClass,
      element,
      inputGroupAppend(withSticker),
    )

  def inputGroupPrepend(element: HtmlElement) = span(cls("input-group-prepend")).amend(element)

  def inputGroupAppend(element: HtmlElement) = span(cls("input-group-append")).amend(element)

  def inputGroupText(text: String) = span(cls("input-group-text"), text)

  //val input_group_lg = "input-group-lg"

  // for multiple files, add multiple := true attr
  def fileInput(todo: Input => Unit) =
    input(
      idAttr := "fileinput",
      `type` := "file",
      inContext(thisNode => onChange --> { _ => todo(thisNode) })
    )

  // CHECKBOX
  def checkbox(isChecked: Boolean) = input(`type` := "checkbox", checked := isChecked)

  trait Displayable {
    def name: String
  }

  // BUTTONS

  def linkButton(content: String, link: String, buttonStyle: HESetters = tool.bootstrapnative.bsn.btn_secondary, openInOtherTab: Boolean = true) =
    a(buttonStyle, href := link, role := "button", target := {
      if (openInOtherTab) "_blank" else ""
    }, content)

  // Clickable span containing a glyphicon and a text
  def glyphSpan(glyphicon: HESetters, modifier: Modifier[HtmlElement] = emptyMod, text: String = "") =
    span(text, glyphicon, cursor.pointer, aria.hidden := true, modifier)


  // Close buttons
  def closeButton(dataDismiss: String, todo: () => Unit = () => {}) =
    button(
      onClick --> { _ => todo() }, cls("close"), aria.label := "Close", dataAttr("dismiss") := dataDismiss,
      span(aria.hidden := true, "&#215")
    )


  //TOGGLE BUTTON
  case class ToggleState[T](t: T, text: String, cls: String, todo: T => Unit = (_: T) => {})

  case class ToggleButtonState[T](state: ToggleState[T], activeState: Boolean, unactiveState: ToggleState[T], onToggled: () => Unit, modifiers: HESetters, withCaret: Boolean) {

    val toggled = Var(activeState)

    lazy val element = button(`type` := "button",
      cls <-- toggled.signal.map(t =>
        if (t) state.cls
        else unactiveState.cls
      ),
      child <-- toggled.signal.map { t =>
        div(if (t) state.text else unactiveState.text,
          if (withCaret) span(tool.bootstrapnative.bsn.glyph_right_caret) else emptyMod)
      },
      onClick --> { _ =>
        toggled.update(!_)
        onToggled()
      },
    ).amend(modifiers: _*)

  }

  def toggle[T](activeState: ToggleState[T], default: Boolean, unactiveState: ToggleState[T], onToggled: () => Unit = () => {}, modifiers: HESetters = emptySetters, withCaret: Boolean = true) = {
    ToggleButtonState(activeState, default, unactiveState, onToggled, modifiers, withCaret)
  }


  case class RadioButtons[T](states: Seq[ToggleState[T]], activeStates: Seq[ToggleState[T]], unactiveStateClass: String, modifiers: HESetters = emptySetters) {

    lazy val active = Var(activeStates)

    lazy val element = div(tool.bootstrapnative.bsnsheet.btnGroup,
      for (rb <- states) yield {
        val rbStateCls = active.signal.map(_.filter(_ == rb).headOption.map(_.cls).getOrElse(unactiveStateClass))

        button(
          rb.text,
          cls <-- rbStateCls,
          onClick --> { _ =>
            active.update { ac =>
              val id = ac.indexOf(rb)
              if (id == -1) ac.appended(rb)
              else ac.patch(id, Nil, 1)
            }
            rb.todo(rb.t)
          }
        )
      }
    )
  }


  def radio[T](buttons: Seq[ToggleState[T]], activeStates: Seq[ToggleState[T]], unactiveStateClass: String, radioButtonsModifiers: HESetters = emptySetters) =
    RadioButtons(buttons, activeStates, unactiveStateClass, radioButtonsModifiers).element


  // RADIO
  enum SelectionSize:
    case DefaultLength, Infinite

  case class ExclusiveRadioButtons[T](buttons: Seq[ToggleState[T]], unactiveStateClass: String, defaultToggles: Seq[Int], selectionSize: SelectionSize = SelectionSize.DefaultLength, radioButtonsModifiers: HESetters) {

    val selected: Var[Seq[ToggleState[T]]] = Var(defaultToggles.map(buttons(_)))

    lazy val element = div(tool.bootstrapnative.bsnsheet.btnGroup, tool.bootstrapnative.bsnsheet.btnGroupToggle, dataAttr("toggle") := "buttons",
      buttons.zipWithIndex.map { case (rb, index) =>
        child <-- selected.signal.map { as =>
          val isInSelection = as.filter(_ == rb) //.map(_.cls)

          val bCls = isInSelection match {
            case Seq() => unactiveStateClass
            case _ => rb.cls
          }
          val isActive = rb == as.last

          label(
            cls := bCls,
            cls.toggle("focus active") := isActive,
            input(`type` := "radio"/*, name := "options"*/, idAttr := s"option${index + 1}", checked := isActive),
            rb.text,
            onClick --> { _ =>
              selected.update(as => {
                val li = (as :+ rb).distinct
                selectionSize match {
                  case SelectionSize.DefaultLength => if (li.size == defaultToggles.size) li else li.drop(1)
                  case SelectionSize.Infinite => if (as.contains(rb)) as.filterNot(_ == rb) else li
                }
              }
              )
              rb.todo(rb.t)
            }
          )
        }
      }
    )
  }

  def exclusiveRadio[T](buttons: Seq[ToggleState[T]], unactiveStateClass: String, defaultToggle: Int, radioButtonsModifiers: HESetters = emptySetters) =
    exclusiveRadios(buttons, unactiveStateClass, Seq(defaultToggle), SelectionSize.DefaultLength, radioButtonsModifiers)

  def exclusiveRadios[T](buttons: Seq[ToggleState[T]], unactiveStateClass: String, defaultToggles: Seq[Int], selectionSize: SelectionSize = SelectionSize.DefaultLength, radioButtonsModifiers: HESetters = emptySetters) =
    ExclusiveRadioButtons(buttons, unactiveStateClass, defaultToggles, selectionSize, radioButtonsModifiers)


  //Label decorators to set the label size
  implicit class TypedTagLabel(badge: Span) {
    def size1(modifierSeq: HESetters = emptySetters) = h1(modifierSeq, badge)

    def size2(modifierSeq: HESetters = emptySetters) = h2(modifierSeq, badge)

    def size3(modifierSeq: HESetters = emptySetters) = h3(modifierSeq, badge)

    def size4(modifierSeq: HESetters = emptySetters) = h4(modifierSeq, badge)

    def size5(modifierSeq: HESetters = emptySetters) = h5(modifierSeq, badge)

    def size6(modifierSeq: HESetters = emptySetters) = h6(modifierSeq, badge)
  }


  // PROGRESS BAR
  def progressBar(barMessage: String, ratio: Int) =
    div(tool.bootstrapnative.bsnsheet.progress,
      div(tool.bootstrapnative.bsnsheet.progressBar, width := ratio.toString() + "%", barMessage)
    )


  // BADGE
  def badge(badgeValue: String, badgeStyle: HESetters) = span(cls("badge"), badgeStyle, marginLeft := "4", badgeValue)

  //BUTTON GROUP
  def buttonGroup = div(tool.bootstrapnative.bsn.btnGroup)

  def buttonToolBar = div(tool.bootstrapnative.bsn.btnToolbar, role := "toolbar")

  //MODAL
  case class ModalDialog(modalHeader: HtmlElement,
                         modalBody: HtmlElement,
                         modalFooter: HtmlElement,
                         modifiers: HESetters = emptySetters,
                         onopen: () => Unit = () => {},
                         onclose: () => Unit = () => {}) {

    lazy val render = {
      val d = div(
        tool.bootstrapnative.bsnsheet.modal, tool.bootstrapnative.bsn.fade, role := "dialog", aria.hidden := true, aria.labelledBy := randomId,
        div(tool.bootstrapnative.bsnsheet.modalDialog, modifiers,
          div(tool.bootstrapnative.bsn.modalContent,
            div(tool.bootstrapnative.bsnsheet.modalHeader, modalHeader),
            div(tool.bootstrapnative.bsnsheet.modalBody, modalBody),
            div(tool.bootstrapnative.bsnsheet.modalFooter, modalFooter)
          )
        )
      )

      org.scalajs.dom.document.body.appendChild(d.ref)
      d
    }


    lazy val modal = new tool.bootstrapnative.BSN.Modal(render.ref)

    def show = {
      modal.show()
      onopen()
    }

    def hide = {
      modal.hide()
      onclose()
    }

    def toggle = modal.toggle()

  }

  //  def modalDialog(modalHeader: HtmlElement, modalBody: HtmlElement, modalFooter: HtmlElement, onopen: () => Unit, onclose: () => Unit, modifiers: HESetters = emptySetters) =
  //    ModalDialog(modalHeader, modalBody, modalFooter, modifiers, onopen, onclose)


  // NAVS
  case class NavItem(contentDiv: HtmlElement,
                     val todo: () ⇒ Unit = () ⇒ {},
                     extraRenderPair: HESetters = emptySetters,
                     activeDefault: Boolean = false,
                     toRight: Boolean = false) {

    val active: Var[Boolean] = Var(activeDefault)

    val render = li(
      cls := tool.bootstrapnative.bsn.nav_item,

      a(href := "#",
        cls := tool.bootstrapnative.bsn.nav_link,
        child <-- active.signal.map { _ => span(cls := "sr-only", "(current)") },
        cls.toggle("active") <-- active.signal,
        lineHeight := "35px",
        onClick --> { _ =>
          todo()
        },
        contentDiv,
        cls.toggle("active") <-- active.signal,
        extraRenderPair
      )
    )

    def right = copy(toRight = true)
  }

  def navItem(content: HtmlElement,
              todo: () => Unit = () => {},
              extraRenderPair: HESetters = emptySetters,
              activeDefault: Boolean = false,
              alignRight: Boolean = false) =
    NavItem(content: HtmlElement, todo, extraRenderPair, activeDefault, alignRight)


  def stringNavItem(content: String, todo: () ⇒ Unit = () ⇒ {}, activeDefault: Boolean = false): NavItem =
    navItem(span(content), todo, activeDefault = activeDefault)

  def navBar(classPair: HESetters, contents: NavItem*) = new NavBar(classPair, None, contents)

  case class NavBarBrand(src: String, modifierSeq: HESetters, todo: () => Unit, alt: String)

  case class NavBar(classPair: HESetters, brand: Option[NavBarBrand], contents: Seq[NavItem]) {

    val navId = randomId

    def render: HtmlElement = {

      val sortedContents = contents.partition {
        _.toRight
      }

      def buildUL(cts: Seq[NavItem], modifier: HESetters = emptySetters) =
        ul(tool.bootstrapnative.bsn.navbar_nav, modifier,
          cts.map { c ⇒
            c.render.amend(onClick --> { _ ⇒
              contents.foreach {
                _.active.set(false)
              }
              c.active.set(true)
            })
          }
        )

      val content = div(cls := "collapse navbar-collapse", idAttr := navId,
        buildUL(sortedContents._2, bsmargin.r.auto),
        buildUL(sortedContents._1, bsmargin.l.auto)
      )

      navTag(tool.bootstrapnative.bsn.navbar, tool.bootstrapnative.bsn.navbar_expand_lg, classPair,
        for {
          b <- brand
        } yield {
          div(
            a(tool.bootstrapnative.bsn.navbar_brand, href := "#", padding := "0",
              img(b.modifierSeq, cursor.pointer, alt := b.alt, src := b.src, onClick --> {
                _ => b.todo()
              })
            ),
            button(cls := "navbar-toggler",
              `type` := "button",
              dataAttr("toggle") := "collapse", dataAttr("target") := s"#$navId",
              aria.controls := navId, aria.expanded := false, aria.label := "Toggle navigation",
              span(cls := "navbar-toggler-icon")
            )
          )
        }, content
      )
    }

    def withBrand(src: String, modifierSeq: HESetters = emptySetters, todo: () => Unit = () => {}, alt: String = "") = copy(brand = Some(NavBarBrand(src, modifierSeq, todo, alt)))

  }

  //POPOVER
  type TypedContent = String | HtmlElement

  case class PopoverBuilder(element: HtmlElement,
                            innerElement: HtmlElement,
                            position: PopupPosition = Bottom,
                            trigger: PopupType = HoverPopup,
                            title: Option[String] = None,
                            dismissible: Boolean = false) {
    lazy val render = element.amend(
      dataAttr("toggle") := "popover",
      dataAttr("html") := "true",
      dataAttr("content") := innerElement.ref.innerHTML,
      dataAttr("placement") := position.value,
      dataAttr("trigger") := {
        trigger match {
          case ClickPopup => "focus"
          case Manual => "manual"
          case _ => "hover"
        }
      },
      title.map(dataAttr("title") := _).getOrElse(emptyMod),
      dataAttr("dismissible") := dismissible.toString,
      onClick --> (_ => popover.show()),
    )

    lazy val popover: tool.bootstrapnative.BSN.Popover = new tool.bootstrapnative.BSN.Popover(render.ref /*, scalajs.js.Dynamic.literal("title" -> "euinesaurtie")*/)

    def show = popover.show()

    def hide = popover.hide()

    def toggle = popover.toggle()

  }

  //TOOLTIP
  class TooltipBuilder(element: HtmlElement,
                       text: String,
                       position: PopupPosition = Bottom,
                       condition: () => Boolean = () => true) {

    val render: HtmlElement = {
      if (condition())
        element.amend(
          dataAttr("placement") := position.value,
          dataAttr("toggle") := "tooltip",
          dataAttr("original-title") := text,
          onMouseOver --> { _ => tooltip.show }
        )
      else element
    }

    val tooltip = new tool.bootstrapnative.BSN.Tooltip(render.ref)

    def hide = tooltip.hide()
  }

  implicit class PopableTypedTag(element: HtmlElement) {

    def tooltip(text: String,
                position: PopupPosition = Bottom,
                condition: () => Boolean = () => true) = {
      new TooltipBuilder(element, text, position, condition).render
    }

    def popover(content: HtmlElement,
                position: PopupPosition = Bottom,
                trigger: PopupType = HoverPopup,
                title: Option[String] = None,
                dismissible: Boolean = false
               ) =
      PopoverBuilder(element, content, position, trigger, title, dismissible)
  }

  //  //DROPDOWN
  implicit class SelectableSeqWithStyle[T](s: Seq[T]) {
    def options(defaultIndex: Int = 0,
                key: HESetters = emptySetters,
                naming: T => String,
                onclose: () => Unit = () => {},
                onclickExtra: () ⇒ Unit = () ⇒ {},
                decorations: Map[T, HESetters] = Map(),
                fixedTitle: Option[String] = None) =
      tool.bootstrapnative.Selector.options(s, defaultIndex, key, naming, onclose, onclickExtra, decorations, fixedTitle)

  }

  // COLLAPSERS
  implicit class TagCollapserOnClick[S <: HtmlElement](trigger: S) {
    def expandOnclick[T <: HtmlElement](inner: T) = {

      val clicked: Var[Boolean] = Var(false)
      div(
        trigger.amend(
          onClick --> { _ =>
            clicked.update(!_)
          }
        ),
        clicked.signal.expand(inner)
      )
    }
  }

  implicit class TTagCollapserWithReactive(r: Signal[Boolean]) {

    def expand(inner: HtmlElement) = {
      div(overflow.hidden,
        transition := "height 300ms",
        height <-- r.map { rr =>
          if (rr) inner.ref.style.height
          else "0px"
        },
        inner
      )
    }
  }


  // TABS
  type TabID = String

  case class Tab[T](t: T,
                    title: HtmlElement,
                    content: HtmlElement,
                    active: Boolean = false,
                    onClicked: () => Unit = () => {},
                    onAdded: () => Unit = () => {},
                    onRemoved: () => Unit = () => {},
                    tabID: TabID = randomId,
                    refID: String = randomId)

  object Tabs {
    def tabs[T](initialTabs: Seq[Tab[T]] = Seq(), isClosable: Boolean = false, tabStyle: HESetters = tool.bootstrapnative.bsnsheet.navTabs) = TabHolder(initialTabs, isClosable, 0, (tab: Tab[T]) => {}, tabStyle)


    //    def defaultSortOptions: (Var[Seq[Tab]], Int => Unit) => SortableOptions = (ts: Var[Seq[Tab]], setActive: Int => Unit) =>
    //      SortableOptions.onEnd(
    //        (event: EventS) ⇒ {
    //          val oldI = event.oldIndex.asInstanceOf[Int]
    //          val newI = event.newIndex.asInstanceOf[Int]
    //          ts.update(cur => cur.updated(oldI, cur(newI)).updated(newI, cur(oldI)))
    //          // ts() = ts.now.updated(oldI, ts.now(newI)).updated(newI, ts.now(oldI))
    //          setActive(newI)
    //        }
    //      )

    case class TabHolder[T](tabs: Seq[Tab[T]], isClosable: Boolean, initIndex: Int /*, sortableOptions: Option[(Var[Seq[Tab]], Int => Unit) => SortableOptions]*/ , onActivation: Tab[T] => Unit, tabStyle: HESetters) {
      def add(tab: Tab[T]): TabHolder[T] = {
        val ts = copy(tabs = this.tabs :+ tab)
        tab.onAdded()
        ts
      }

      def closable = copy(isClosable = true)

      def initialIndex(index: Int) = copy(initIndex = index)

      //  def withSortableOptions(options: (Var[Seq[Tab]], Int => Unit) => SortableOptions) = copy(sortableOptions = Some(options))

      //  def onActivation(onActivation: Tab[T] => Unit = Tab => {}) = copy(onActivation = onActivation)

      def build = {
        Tabs(tabs, isClosable, tabStyle = tabStyle)
      }
    }

  }

  case class Tabs[T](initialTabs: Seq[Tab[T]], isClosable: Boolean, tabStyle: HESetters) {

    val tabs = Var(initialTabs)

    if (!initialTabs.map(_.active).exists(_ == true))
      setFirstActive

    //def activeTab = tabs.now().filter(_.active).headOption
    val activeTab = tabs.signal.map { t =>
      t.filter {
        _.active
      }
    }


    def setFirstActive = tabs.now().headOption.foreach { t =>
      setActive(t.tabID)
    }

    def setActive(tabID: TabID) = {
      tabs.update { ts =>
        ts.map { t => t.copy(active = t.tabID == tabID) }
      }
    }

    def tab(tabID: TabID) = tabs.now().filter {
      _.tabID == tabID
    }.headOption

    def add(tab: Tab[T], activate: Boolean = true) = {
      tabs.update(t => t :+ tab)
      if (activate) setActive(tab.tabID)
    }

    def isActive(id: TabID) = tab(id).map {
      _.active
    }.getOrElse(false)

    def remove(tabID: TabID) = {
      tab(tabID).map {
        _.onRemoved()
      }

      //Remove tab
      tabs.update(t => t.filterNot(_.tabID == tabID))

      //Fix active tab
      if (tabs.now().length > 0 && tabs.now().map {
        _.active
      }.forall(_ == false)) {
        tabs.update { e =>
          e.head.copy(active = true) +: e.tail
        }
      }
    }

    //def onclose(f: (Tab) => Unit) = copy(onCloseExtra = f)

    lazy val tabClose: HESetters = Seq(
      position := "relative",
      fontSize := "20",
      color := "black",
      right := "-10",
      opacity := "0.3",
      width := "20"
    )

    case class TabRender(tabHeader: Li, tabContent: Div)

    def renderTab(tabID: TabID, initialTab: Tab[T], tabStream: Signal[Tab[T]]): TabRender = {


      val header = li(
        cls := tool.bootstrapnative.bsn.nav_item,
        a(
          cls := tool.bootstrapnative.bsn.nav_link,
          cls.toggle("active") <-- tabStream.map { t =>
            t.active
          },
          a(idAttr := tabID,
            tool.bootstrapnative.bsn.tab_role,
            cursor.pointer,
            dataAttr("toggle") := "tab",
            dataAttr("height") := "true",
            aria.controls <-- tabStream.map { t => t.refID },
            onClick --> { _ =>
              setActive(tabID)
              initialTab.onClicked()
            },
            if (isClosable) button(cls := "close", tabClose, `type` := "button", "×",
              onClick --> { e =>
                remove(initialTab.tabID)
                e.stopPropagation()
              }) else span(),
            child <-- tabStream.map {
              _.title
            }
          )
        )
      )

      val tabDiv =
      // div(idAttr <-- tabStream.map { t => t.refID },
        div(tool.bootstrapnative.bsn.tab_content,
          div(tool.bootstrapnative.bsn.tab_panel_role,
            tool.bootstrapnative.bsn.tab_pane, tool.bootstrapnative.bsn.fade,
            cls.toggle("active show") <-- tabStream.map(_.active),
            child <-- tabStream.map { t => t.content }
          )
        )

      //FIXME
      //  Sortable(header, sortableOptions(tabs, setActive))

      TabRender(header, tabDiv)
    }

    def render: Div = render("")

    def render(contentClass: String): Div = {
      div(children <-- tabs.signal.split(_.tabID)(renderTab).map { tr =>
        Seq(
          ul(tool.bootstrapnative.bsn.nav, tabStyle, tr.map(_.tabHeader)),
          div(tool.bootstrapnative.bsn.tab_content, paddingTop := "10", tr.map(_.tabContent), cls := contentClass)
        )
      })
    }
  }

  //
  //  //TABLE

  def elementTable(rows: Seq[Row] = Seq()) = ElementTableBuilder(rows)

  def dataTable(rows: Seq[Seq[DataContent]] = Seq()) = DataTableBuilder(rows)

  //  // FORMS
  trait FormTag {
    def tag: HtmlElement
  }

  trait LabeledFormTag extends FormTag {
    def label: Label
  }

  implicit def htmlElementToFormTag(t: HtmlElement): FormTag = new FormTag {
    val tag: HtmlElement = t
  }

  implicit class LabelForModifiers(m: HtmlElement) {
    def withLabel(title: String, labelStyle: HESetters = emptySetters): LabeledFormTag = new LabeledFormTag {
      val label: Label = com.raquo.laminar.api.L.label(title, labelStyle, paddingRight := "5")

      val tag: HtmlElement = m
    }
  }

  private def insideForm(elements: Seq[FormTag]) =
    for {
      ft <- elements
    } yield {
      div(tool.bootstrapnative.bsnsheet.formGroup, paddingRight := "5",
        ft match {
          case lft: LabeledFormTag => lft.label
          case _ => span()
        },
        ft.tag
      )
    }


  def vForm(elements: FormTag*): HtmlElement = vForm(emptySetters, elements: _*)


  def vForm(heSetters: HESetters, elements: FormTag*): HtmlElement =
    div(heSetters :+ tool.bootstrapnative.bsnsheet.formVertical, insideForm(elements))


  def hForm(formTags: FormTag*): HtmlElement = hForm(emptySetters, formTags: _*)

  def hForm(heSetters: HESetters, formTags: FormTag*): HtmlElement = {
    form(tool.bootstrapnative.bsn.formInline, heSetters, insideForm(formTags))
  }

  //TOAST
  type ToastPosition = HESetters
  type ToastID = String

  case class ToastHeader(text: String, comment: String = "", backgroundColor: String = "#fff")

  case class Toast(header: ToastHeader, bodyText: String, toastPosition: ToastPosition = tool.bootstrapnative.bsn.bottomRightPosition, delay: Option[Int] = None, toastID: ToastID = randomId)

  def toast(toastHeader: ToastHeader, bodyText: String, toastPosition: ToastPosition = tool.bootstrapnative.bsn.bottomRightPosition, delay: Option[Int] = None) =
    Toast(toastHeader, bodyText, toastPosition, delay)

  case class ToastStack(toastPosition: ToastPosition, unstackOnClose: Boolean, initialToasts: Seq[Toast]) {

    val toasts = Var(initialToasts)
    val activeToasts = Var(Seq[Toast]())

    def toast(toastID: ToastID) = toasts.now().filter(_.toastID == toastID)

    def stack(toast: Toast) =
      if (!toasts.now().exists(_ == toast))
        toasts.update(ts => ts :+ toast)

    def unstack(toast: Toast) = toasts.update(ts => ts.filterNot(_ == toast))

    def show(toast: Toast) =
      if (!activeToasts.now().exists(_ == toast)) {
        activeToasts.update(ts => ts :+ toast)
        toast.delay.foreach { d =>
          scalajs.js.timers.setTimeout(d)(hide(toast))
        }
      }

    def stackAndShow(toast: Toast) = {
      stack(toast)
      show(toast)
    }

    def hide(toast: Toast) = activeToasts.update(at => at.filterNot(_ == toast))


    def toastRender(toastID: ToastID, initialToast: Toast, toastStream: Signal[Toast]): Div = {

      val isActive = activeToasts.signal.map { ts => ts.contains(initialToast) }

      div(
        tool.bootstrapnative.bsnsheet.toastCls, role := "alert", aria.live := "assertive", aria.atomic := true, dataAttr("animation") := "true",
        cls <-- isActive.map { a =>
          if (a) "fade show" else "fade hide"
        },
        div(tool.bootstrapnative.bsn.toastHeader, backgroundColor := initialToast.header.backgroundColor,
          strong(bsmargin.r.auto, initialToast.header.text),
          small(initialToast.header.comment),
          button(`type` := "button", bsmargin.l.two, bsmargin.b.one, cls := "close", dataAttr("dismiss") := "toast", aria.label := "Close",
            span(aria.hidden := true, "×"),
            onClick --> { e =>
              hide(initialToast)
              if (unstackOnClose)
                unstack(initialToast)
            }
          )
        ),
        div(tool.bootstrapnative.bsn.toastBody, initialToast.bodyText)
      )
    }

    val render = {
      div(
        toastPosition,
        children <-- toasts.signal.split(_.toastID)(toastRender)
      )
    }
  }

  def toastStack(toastPosition: ToastPosition, unstackOnClose: Boolean, toasts: Toast*) = ToastStack(toastPosition: ToastPosition, unstackOnClose, toasts)
}




