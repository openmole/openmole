var factoryMap = new Map();
var fakeMap = Map();
var fake2Map = Map();

var inst = new org.openmole.gui.plugin.task.groovy.client.GroovyTaskDataUI();

fakeMap.set("aa","oo")
fake2Map.set(inst,"oo")
factoryMap.set("org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData", inst);

var inst = ScalaJS.c.Lorg_openmole_gui_plugin_task_groovy_client_GroovyTaskDataUI();

var factoryMap = {
"org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData": inst
};
