
importPackage(Packages.arc.files);

this.boolc = method => new Boolc({get: method})

require('scene/ui/updateData')

var useable = require('testFi').useable;
this.contArr = ['tester', 'makeMod', 'settings', 'other'].map(str => {
	// Log.info('Loaded ' + modName + ' \'s function ' + str);
	if (!Core.settings.get(modName + '-load-' + str, true)) return
	/* 不让游戏崩溃 */
	try {
		var cont = require('content/' + str).cont;
		return !cont.needFi || useable ? cont : null;
	} catch (err) {
		Log.err('' + err);
	}
	return null;
});
// this.bc = require('func/buildContent')

require('scene/ui/frag')