package org.openmole.gui.client.tool.bootstrapnative

/*
 * Copyright (C) 30/03/16 // mathieu.leclaire@openmole.org
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


import com.raquo.laminar.api.L.{*, given}

package stylesheet {

  //import com.raquo.laminar.defs.ReactiveComplexHtmlKeys
  import com.raquo.laminar.nodes
  import com.raquo.laminar.nodes.{ReactiveElement, ReactiveHtmlElement}

  //package object bootstrap extends BootstrapPackage

  trait BootstrapPackage {


    private def toGlyphicon(s: String) = cls(s"$s")

    private def toBadge(s: String) = cls(s"badge $s")

    private def toButton(s: String) = cls(s"btn $s")

    private def toAlert(s: String) = cls(s"alert $s")

    private def toNav(s: String) = cls(s"nav $s")

    private def toTable(s: String) = cls(s"table $s")

    //    implicit class ExtendBSButton(bsButton: BSButton) {
    //      def outlined = bsButton.   .key.name.replaceFirst("btn-","btn-outline")
    //    }

    object spacing {
      type BSSpacing = String

      implicit class ABSMargin(bsSpacing: BSSpacing) {
        private def pattern(suffix: String) = cls := s"$bsSpacing-$suffix"

        def zero = pattern("0")
        def one = pattern("1")
        def two = pattern("2")
        def three = pattern("3")
        def four = pattern("4")
        def five = pattern("5")

        def auto = pattern("auto")
      }

      //SPACING
      object bsmargin {
        // right, left, top, bottom, left and right (x), top and bottom (y)
        def r: BSSpacing = "mr"

        def l: BSSpacing = "ml"

        def t: BSSpacing = "mt"

        def b: BSSpacing = "mb"

        def x: BSSpacing = "mx"

        def y: BSSpacing = "my"
      }

      object bspadding {
        // right, left, top, bottom, left and right (x), top and bottom (y)
        def r: BSSpacing = "pr"

        def l: BSSpacing = "pl"

        def t: BSSpacing = "pt"

        def b: BSSpacing = "pb"

        def x: BSSpacing = "px"

        def y: BSSpacing = "py"
      }
    }



    //GHYPHICONS
    lazy val glyph_edit = toGlyphicon("bi-pencil-fill")
    lazy val glyph_edit2 = toGlyphicon("bi-edit")
    lazy val glyph_save = toGlyphicon("bi-save")
    lazy val glyph_trash = toGlyphicon("bi-trash")
    lazy val glyph_plus = toGlyphicon("bi-plus")
    lazy val glyph_plus_sign = toGlyphicon("bi-plus-sign")
    lazy val glyph_minus_sign = toGlyphicon("bi-minus-sign")
    lazy val glyph_minus = toGlyphicon("bi-minus")
    lazy val glyph_ok = toGlyphicon("bi-ok")
    lazy val glyph_question = toGlyphicon("bi-question-sign")
    lazy val glyph_file = toGlyphicon("bi-file")
    lazy val glyph_folder_close = toGlyphicon("bi-folder-close")
    lazy val glyph_home = toGlyphicon("bi-home")
    lazy val glyph_upload = toGlyphicon("bi-cloud-upload")
    lazy val glyph_download = toGlyphicon("bi-download")
    lazy val glyph_download_alt = toGlyphicon("bi-download-alt")
    lazy val glyph_settings = toGlyphicon("bi-gear-fill")
    lazy val glyph_off = toGlyphicon("bi-off")
    lazy val glyph_lightning = toGlyphicon("bi-lightning")
    lazy val glyph_flag = toGlyphicon("bi-flag")
    lazy val glyph_remove = toGlyphicon("bi-x-square")
    lazy val glyph_road = toGlyphicon("bi-road")
    lazy val glyph_heart = toGlyphicon("bi-suit-heart-fill")
    lazy val glyph_list = toGlyphicon("bi-list")
    lazy val glyph_stats = toGlyphicon("bi-stats")
    lazy val glyph_refresh = toGlyphicon("bi-arrow-repeat")
    lazy val glyph_repeat = toGlyphicon("bi-repeat")
    lazy val glyph_lock = toGlyphicon("bi-lock-fill")
    lazy val glyph_archive = toGlyphicon("bi-compressed")
    lazy val glyph_market = toGlyphicon("bi-shopping-cart")
    lazy val glyph_info = toGlyphicon("bi-info-sign")
    lazy val glyph_plug = toGlyphicon("bi-plug-fill")
    lazy val glyph_exclamation = toGlyphicon("bi-exclamation-sign")
    lazy val glyph_comment = toGlyphicon("bi-comment")
    lazy val glyph_upload_alt = toGlyphicon("bi-upload")
    lazy val glyph_arrow_right = toGlyphicon("bi-arrow-right")
    lazy val glyph_arrow_left = toGlyphicon("bi-arrow-left")
    lazy val glyph_arrow_right_and_left = toGlyphicon("bi-resize -horizontal")
    lazy val glyph_filter = toGlyphicon("bi-filter")
    lazy val glyph_copy = toGlyphicon("bi-copy")
    lazy val glyph_paste = toGlyphicon("bi-paste")
    lazy val glyph_time = toGlyphicon("bi-time")
    lazy val glyph_alph_sorting = toGlyphicon("bi-sort-by-alphabet")
    lazy val glyph_sort_down = "bi-sort-down"
    lazy val glyph_sort_down_alt = "bi-sort-down-alt"
    lazy val glyph_triangle_down = toGlyphicon("bi-caret-down-fill")
    lazy val glyph_triangle_up = toGlyphicon("bi-caret-up-fill")
    lazy val glyph_chevron_left = toGlyphicon("bi-chevron-left")
    lazy val glyph_chevron_right = toGlyphicon("bi-chevron-right")
    lazy val glyph_menu_hamburger = toGlyphicon("bi-menu-hamburger")
    lazy val glyph_right_caret = toGlyphicon("bi-caret-right-fill")
    lazy val glyph_three_dots = toGlyphicon("bi-three-dots-vertical")

    //NAVBARS
    lazy val nav = cls("nav")
    lazy val navbar = cls("navbar")
    lazy val navbar_nav = cls("navbar-nav")
    lazy val navbar_expand_lg = cls("navbar-expand-lg")
    lazy val navbar_light = cls("navbar-light")
    lazy val bg_light = cls("bg-light")
    lazy val navTabs = cls("nav-tabs")
    lazy val navbar_default = cls("navbar-default")
    lazy val navbar_inverse = cls("navbar-inverse")
    lazy val navbar_fixedTop = cls("navbar-fixed-top")
    lazy val navbar_pills = cls("nav-pills")
    lazy val navbar_form = cls("navbar-form")
    lazy val navbar_right = cls("navbar-right")
    lazy val navbar_left = cls("navbar-left")
    lazy val navbar_header = cls("navbar-header")
    lazy val navbar_brand = cls("navbar-brand")
    lazy val navbar_btn = cls("navbar-btn")
    lazy val navbar_collapse = cls("collapse navbar-collapse")

    //LABELS
    lazy val badge_light = toBadge("badge-light")
    lazy val badge_primary = toBadge("badge-primary")
    lazy val badge_success = toBadge("badge-success")
    lazy val badge_info = toBadge("badge-info")
    lazy val badge_warning = toBadge("badge-warning")
    lazy val badge_danger = toBadge("badge-danger")
    lazy val badge_dark = toBadge("badge-dark")
    lazy val badge_secondary = toBadge("badge-secondary")

    lazy val controlLabel = cls("control-label")


    //BUTTONS
    lazy val btn = cls("btn")
    lazy val btn_secondary_string = "btn btn-secondary"
    lazy val btn_outline_secondary_string = "btn btn-outline-secondary"
    lazy val btn_secondary = toButton(s"btn $btn_secondary_string")

    lazy val btn_primary_string = "btn btn-primary"
    lazy val btn_outline_primary_string = "btn btn-outline-primary"
    lazy val btn_primary = toButton(s"btn $btn_primary_string")

    lazy val btn_light = toButton("btn btn-light")
    lazy val btn_light_outline = toButton("btn btn-outline-light")

    lazy val btn_success_string = "btn btn-success"
    lazy val btn_success = toButton(btn_success_string)

    lazy val btn_info_string = "btn btn-info"
    lazy val btn_info = toButton(btn_info_string)

    lazy val btn_warning_string = "btn btn-warning"
    lazy val btn_warning = toButton(btn_warning_string)

    lazy val btn_danger_string = "btn btn-danger"
    lazy val btn_danger = toButton(btn_danger_string)

    lazy val btn_primary_outline = toButton("btn-outline-primary")
    lazy val btn_secondary_outline = toButton("btn-outline-secondary")
    lazy val btn_success_outline = toButton("btn-outline-success")
    lazy val btn_info_outline = toButton("btn-outline-info")
    lazy val btn_warning_outline = toButton("btn-outline-warning")
    lazy val btn_danger_outline = toButton("btn-outline-danger")

    lazy val btn_large = toButton("btn-lg")
    lazy val btn_medium = toButton("btn-md")
    lazy val btn_small = toButton("btn-sm")
    lazy val btn_test = toButton("myButton")
    lazy val btn_right = toButton("pull-right")

    lazy val btnGroup = cls("btn-group")
    lazy val btnGroupToggle = cls("btn-group-toggle")
    lazy val btnToolbar = cls("btn-toolbar")


    //ALERTS
    lazy val alert_success = toAlert("alert-success")
    lazy val alert_info = toAlert("alert-info")
    lazy val alert_warning = toAlert("alert-warning")
    lazy val alert_danger = toAlert("alert-danger")

    //MODALS
    lazy val modal = cls("modal")
    lazy val fade = cls("fade")
    lazy val modalDialog = cls("modal-dialog")
    lazy val modalContent = cls("modal-content")
    lazy val modalHeader = cls("modal-header")
    lazy val modalInfo = cls("modal-info")
    lazy val modalBody = cls("modal-body")
    lazy val modalFooter = cls("modal-footer")

    //NAVS
    lazy val tabsClass = toNav("nav-tabs")
    lazy val justified_tabs = toNav("nav-tabs nav-justified")
    lazy val pills = toNav("nav-pills")
    lazy val stacked_pills = toNav("nav-pills nav-stacked")
    lazy val justified_pills = toNav("nav-pills nav-justified")
    lazy val inline_list = toNav("list-inline")
    lazy val nav_bar = toNav("navbar-nav")
    lazy val regular_nav = toNav("nav-list")
    lazy val panel_nav = toNav("panel panel-primary")
    lazy val presentation_role = role := "presentation"
    lazy val tab_panel_role = role := "tabpanel"
    lazy val tab_list_role = role := "tablist"
    lazy val tab_role = role := "tab"
    lazy val tab_content = cls("tab-content")
    lazy val tab_pane = cls("tab-pane")
    lazy val nav_item = "nav-item"
    lazy val nav_link = "nav-link"

    //TABLES
    lazy val header_no_color = emptyMod
    lazy val default_header = cls("thead-default")
    lazy val default_inverse = cls("thead-inverse")
    lazy val default_table = toTable("")
    lazy val inverse_table = toTable("table-inverse")
    lazy val striped_table = toTable("table-striped")
    lazy val bordered_table = toTable("table-bordered")
    lazy val hover_table = toTable("table-hover")

    //GRIDS
    def colBS(nbCol: Int) = cls(s"col-$nbCol")

    def colMDOffset(offsetSize: Int) = cls(s"col-md-offset-$offsetSize")

    lazy val row = cls("row")
    lazy val colSM = cls("col-sm")


    //PANELS
    lazy val panelClass = cls("panel")
    lazy val panelDefault = cls("panel-default")
    lazy val panelHeading = cls("panel-heading")
    lazy val panelTitle = cls("panel-title")
    lazy val panelBody = cls("panel-body")


    //TABLES
    lazy val tableClass = cls("table")
    lazy val bordered = cls("table-bordered")
    lazy val striped = cls("table-striped")
    lazy val active = cls("active")
    lazy val success = cls("success")
    lazy val danger = cls("danger")
    lazy val warning = cls("warning")
    lazy val info = cls("info")


    //INPUTS
    lazy val inputGroupClass = cls("input-group")
    lazy val inputGroupButtonClass = cls("input-group-btn")
    lazy val inputGroupAddonClass = cls("input-group-addon")

    //FORMS
    lazy val formControl = cls("form-control")
    lazy val formGroup = cls("form-group")
    lazy val formInline = cls("form-inline")
    lazy val formHorizontal = cls("form-horizontal")
    lazy val formVertical = cls("form-vertical")


    //OTHERS
    lazy val dropdown = cls("dropdown")
    lazy val dropdownMenu = cls("dropdown-menu")
    lazy val dropdownToggle = cls("dropdown-toggle")
    lazy val progress = cls("progress")
    lazy val progressBar = cls("progress-bar")
    lazy val container = cls("container")
    lazy val containerFluid = cls("container-fluid")
    lazy val jumbotron = cls("jumbotron")
    lazy val themeShowcase = cls("theme-showcase")
    lazy val controlGroup = cls("control-group")
    lazy val controls = cls("controls")


    //TOASTS
    lazy val toastCls = cls := "toast"
    lazy val toastHeader = cls := "toast-header"
    lazy val toastBody = cls := "toast-body"

    lazy val bottomRightPosition = Seq(cls := "position-fixed bottom-0 right-0 p-3", zIndex := "5", right := "0", bottom := "0")

  }

}

package stylesheet2 {

  package object bootstrap2 extends Bootstrap2Package

  trait Bootstrap2Package {

    //Exclusive Button Group
    lazy val stringInGroup = Seq(
      height := "30px",
      paddingTop := "3px",
      paddingLeft := "6px",
      paddingRight := "6px"
    )

    lazy val twoGlyphButton = Seq(
      top := "1px",
      height := "30px"
    )

    lazy val stringButton = Seq(
      top := "4px",
      height := "30px"
    )

    lazy val collapseTransition = Seq(
      transition := "height .3s",
      height := "0",
      overflow := "hidden"
    )

    lazy val caret = Seq(
      display.inlineBlock,
      width := "0",
      height := "0",
      marginLeft := "5",
      verticalAlign.middle,
      borderTop := "4px dashed",
      borderRight := "4px solid transparent ",
      borderLeft := "4px solid transparent"
    )

  }

}
