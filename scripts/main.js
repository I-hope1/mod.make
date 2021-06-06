
importPackage(Packages.arc.files);

require('updateData');

var useable = require('testFi').useable;

this.contArr = ['tester', 'makeMod', 'other'].map(str => {
	// Log.info('Loaded ' + modName + ' \'s function ' + str);
	/* 不让游戏崩溃 */
	try {
		var cont = require('content/' + str).cont;
		return !cont.needFi || useable ? cont : null;
	} catch (err) {
		Log.err('' + err);
	}
	return null;
});
require('frag');