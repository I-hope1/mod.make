
importPackage(Packages.arc.files);

this.boolc = method => new Boolc({ get: method })
this.cons2 = method => new Cons2({ get: method })

this[modName + "-contArr"] = [];

const IntStyles = require("func/findClass")("ui.styles");
Events.run(ClientLoadEvent, () => {
	IntStyles.load()
})

require("content/makeMod");
const IntSettings = require("content/settings");

// this.bc = require('func/buildContent')

require('ui/frag')

Core.graphics.setTitle(modName + ": 感谢使用此mod")

if (!IntSettings.getValue("base", "not_show_again")) {
	Log.info("load updateData")
	require('updateData')
}
