exports.useable = true;
try {
	Vars.dataDirectory;
} catch(err) {
	exports.useable = false;
}
