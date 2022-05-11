
importPackage(Packages.arc.files);

this.boolc = method => new Boolc({ get: method })
this.cons2 = method => new Cons2({ get: method })

this[modName + "-contArr"] = [];

const IntStyles = require("func/findClass")("ui.styles");
Events.run(ClientLoadEvent, () => {
	IntStyles.load()
})

require("content/makeMod");
const findClass = require('func/findClass')
const { settings } = findClass("components.dataHandle");

// this.bc = require('func/buildContent')

require('ui/frag')

Core.graphics.setTitle(modName + ": 感谢使用此mod")

if (!settings.getBool("not_show_again")) {
	Log.info("load updateData")
	require('updateData')
}
