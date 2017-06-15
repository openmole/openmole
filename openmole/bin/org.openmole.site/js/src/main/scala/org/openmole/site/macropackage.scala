
      package org.openmole.site

        trait JSPage {
          def name: String
          def file: String
          def details: Seq[JSPage]
          }
        case class JSMainPage(name: String, file: String, details: Seq[JSPage] = Seq()) extends JSPage
        case class JSDocumentationPage(name: String, file: String, details: Seq[JSPage] = Seq(), children: Seq[JSDocumentationPage] = Seq()) extends JSPage

      object JSPages {

        def toJSPage(file: String): Option[JSPage] = all.filter(_.file == file).headOption
      
lazy val documentation = JSDocumentationPage("Documentation", "Documentation.html", children=Seq(documentation_application,documentation_language,documentation_tutorials,documentation_market,documentation_development))
lazy val documentation_application = JSDocumentationPage("Application", "Documentation_Application.html", children=Seq(documentation_application_migration))
lazy val documentation_language = JSDocumentationPage("Language", "Documentation_Language.html", children=Seq(documentation_language_models,documentation_language_samplings,documentation_language_transitions,documentation_language_hooks,documentation_language_environments,documentation_language_sources,documentation_language_methods))
lazy val documentation_tutorials = JSDocumentationPage("Tutorials", "Documentation_Tutorials.html", children=Seq(documentation_tutorials_helloword,documentation_tutorials_resumeworkflow,documentation_tutorials_netlogoheadless,documentation_tutorials_gawithnetlogo,documentation_tutorials_capsule))
lazy val documentation_market = JSDocumentationPage("Market", "Documentation_Market.html")
lazy val documentation_development = JSDocumentationPage("Development", "Documentation_Development.html", children=Seq(documentation_development_compilation,documentation_development_documentation,documentation_development_plugins,documentation_development_branching_model,documentation_development_web_server))
lazy val documentation_application_migration = JSDocumentationPage("Migration", "Documentation_Application_Migration.html")
lazy val documentation_language_models = JSDocumentationPage("Models", "Documentation_Language_Models.html", children=Seq(documentation_language_models_scala,documentation_language_models_java,documentation_language_models_r_script,documentation_language_models_python,documentation_language_models_ccplusplus,documentation_language_models_native,documentation_language_models_netlogo,documentation_language_models_mole))
lazy val documentation_language_samplings = JSDocumentationPage("Samplings", "Documentation_Language_Samplings.html")
lazy val documentation_language_transitions = JSDocumentationPage("Transitions", "Documentation_Language_Transitions.html")
lazy val documentation_language_hooks = JSDocumentationPage("Hooks", "Documentation_Language_Hooks.html")
lazy val documentation_language_environments = JSDocumentationPage("Environments", "Documentation_Language_Environments.html", children=Seq(documentation_language_environments_multithread,documentation_language_environments_ssh,documentation_language_environments_egi,documentation_language_environments_clusters,documentation_language_environments_desktopgrid))
lazy val documentation_language_sources = JSDocumentationPage("Sources", "Documentation_Language_Sources.html")
lazy val documentation_language_methods = JSDocumentationPage("Methods", "Documentation_Language_Methods.html", children=Seq(documentation_language_methods_calibration,documentation_language_methods_sensitivity_analysis,documentation_language_methods_profiles,documentation_language_methods_pse))
lazy val documentation_language_models_scala = JSDocumentationPage("Scala", "Documentation_Language_Models_Scala.html")
lazy val documentation_language_models_java = JSDocumentationPage("Java", "Documentation_Language_Models_Java.html")
lazy val documentation_language_models_r_script = JSDocumentationPage("R Script", "Documentation_Language_Models_R Script.html", details=Seq(documentation_language_models_nativeapi,documentation_language_models_nativepackaging,documentation_language_models_caretroubleshooting))
lazy val documentation_language_models_python = JSDocumentationPage("Python", "Documentation_Language_Models_Python.html", details=Seq(documentation_language_models_nativeapi,documentation_language_models_nativepackaging,documentation_language_models_caretroubleshooting))
lazy val documentation_language_models_ccplusplus = JSDocumentationPage("CCplusplus", "Documentation_Language_Models_CCplusplus.html", details=Seq(documentation_language_models_nativeapi,documentation_language_models_nativepackaging,documentation_language_models_caretroubleshooting))
lazy val documentation_language_models_native = JSDocumentationPage("Native", "Documentation_Language_Models_Native.html", details=Seq(documentation_language_models_nativeapi,documentation_language_models_nativepackaging,documentation_language_models_caretroubleshooting))
lazy val documentation_language_models_netlogo = JSDocumentationPage("NetLogo", "Documentation_Language_Models_NetLogo.html")
lazy val documentation_language_models_mole = JSDocumentationPage("Mole", "Documentation_Language_Models_Mole.html")
lazy val documentation_language_models_nativeapi = JSDocumentationPage("API", "Documentation_Language_Models_NativeAPI.html")
lazy val documentation_language_models_nativepackaging = JSDocumentationPage("Native Packaging", "Documentation_Language_Models_NativePackaging.html")
lazy val documentation_language_models_caretroubleshooting = JSDocumentationPage("CARE Troubleshooting", "Documentation_Language_Models_CARETroubleshooting.html")
lazy val documentation_language_environments_multithread = JSDocumentationPage("Multi-threads", "Documentation_Language_Environments_MultiThread.html")
lazy val documentation_language_environments_ssh = JSDocumentationPage("SSH", "Documentation_Language_Environments_SSH.html")
lazy val documentation_language_environments_egi = JSDocumentationPage("EGI", "Documentation_Language_Environments_EGI.html")
lazy val documentation_language_environments_clusters = JSDocumentationPage("Clusters", "Documentation_Language_Environments_Clusters.html")
lazy val documentation_language_environments_desktopgrid = JSDocumentationPage("Desktop Grid", "Documentation_Language_Environments_DesktopGrid.html")
lazy val documentation_language_methods_calibration = JSDocumentationPage("Calibration", "Documentation_Language_Methods_Calibration.html")
lazy val documentation_language_methods_sensitivity_analysis = JSDocumentationPage("Sensitivity_Analysis", "Documentation_Language_Methods_Sensitivity_Analysis.html")
lazy val documentation_language_methods_profiles = JSDocumentationPage("Profiles", "Documentation_Language_Methods_Profiles.html")
lazy val documentation_language_methods_pse = JSDocumentationPage("PSE", "Documentation_Language_Methods_PSE.html")
lazy val documentation_tutorials_helloword = JSDocumentationPage("Hello World", "Documentation_Tutorials_HelloWord.html")
lazy val documentation_tutorials_resumeworkflow = JSDocumentationPage("Resume workflow", "Documentation_Tutorials_ResumeWorkflow.html")
lazy val documentation_tutorials_netlogoheadless = JSDocumentationPage("NetLogo Headless", "Documentation_Tutorials_NetlogoHeadless.html")
lazy val documentation_tutorials_gawithnetlogo = JSDocumentationPage("GA with NetLogo", "Documentation_Tutorials_GAwithNetLogo.html")
lazy val documentation_tutorials_capsule = JSDocumentationPage("Capsule", "Documentation_Tutorials_Capsule.html")
lazy val documentation_development_compilation = JSDocumentationPage("Compilation", "Documentation_Development_Compilation.html")
lazy val documentation_development_documentation = JSDocumentationPage("Documentation", "Documentation_Development_Documentation.html")
lazy val documentation_development_plugins = JSDocumentationPage("Plugins", "Documentation_Development_Plugins.html")
lazy val documentation_development_branching_model = JSDocumentationPage("Branching model", "Documentation_Development_Branching model.html")
lazy val documentation_development_web_server = JSDocumentationPage("Web Server", "Documentation_Development_Web Server.html")
lazy val index = JSMainPage("index", "index.html")
lazy val getting_started = JSMainPage("getting_started", "getting_started.html")
lazy val who_are_we = JSMainPage("who_are_we", "who_are_we.html")
lazy val faq = JSMainPage("faq", "faq.html")
lazy val communications = JSMainPage("communications", "communications.html")

lazy val all: Seq[JSPage] = Seq(documentation, documentation_application, documentation_language, documentation_tutorials, documentation_market, documentation_development, documentation_application_migration, documentation_language_models, documentation_language_samplings, documentation_language_transitions, documentation_language_hooks, documentation_language_environments, documentation_language_sources, documentation_language_methods, documentation_language_models_scala, documentation_language_models_java, documentation_language_models_r_script, documentation_language_models_python, documentation_language_models_ccplusplus, documentation_language_models_native, documentation_language_models_netlogo, documentation_language_models_mole, documentation_language_models_nativeapi, documentation_language_models_nativepackaging, documentation_language_models_caretroubleshooting, documentation_language_environments_multithread, documentation_language_environments_ssh, documentation_language_environments_egi, documentation_language_environments_clusters, documentation_language_environments_desktopgrid, documentation_language_methods_calibration, documentation_language_methods_sensitivity_analysis, documentation_language_methods_profiles, documentation_language_methods_pse, documentation_tutorials_helloword, documentation_tutorials_resumeworkflow, documentation_tutorials_netlogoheadless, documentation_tutorials_gawithnetlogo, documentation_tutorials_capsule, documentation_development_compilation, documentation_development_documentation, documentation_development_plugins, documentation_development_branching_model, documentation_development_web_server, index, getting_started, who_are_we, faq, communications)

lazy val topPagesChildren = Seq(documentation_language_models_scala, documentation_language_models_java, documentation_language_models_r_script, documentation_language_models_python, documentation_language_models_ccplusplus, documentation_language_models_native, documentation_language_models_netlogo, documentation_language_models_mole, documentation_language_methods_calibration, documentation_language_methods_sensitivity_analysis, documentation_language_methods_profiles, documentation_language_methods_pse, documentation_language_environments_multithread, documentation_language_environments_ssh, documentation_language_environments_egi, documentation_language_environments_clusters, documentation_language_environments_desktopgrid)
                 

}