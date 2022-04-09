
importPackage(Packages.arc.files);

this.boolc = method => new Boolc({ get: method })

this.contArr = []
require("func/IniHandle");
require("content/makeMod");
require("content/settings");

// this.bc = require('func/buildContent')

require('ui/frag')

const IntSettings = require("content/settings");
if (!IntSettings.getValue("base", "not_show_again")) {
	Log.info("load updateData")
	require('updateData')
}
