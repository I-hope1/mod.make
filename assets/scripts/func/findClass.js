module.exports = (() => {
	let NativeJavaClass = Packages.rhino.NativeJavaClass
	let pkgname = "modmake"
	let loader = Vars.mods.mainLoader();
	let scripts = Vars.mods.scripts;
	let { scope, context } = scripts;
	let cache = {}
	return (name, def) => {
		try {
			return cache[name] ||
				(cache[name] = NativeJavaClass(scope, loader.loadClass(pkgname + "." + name)));
		} catch (err) {
			if (def) {
				return def
			} else {
				Log.info(err)
				return null
			}
		}
	}
})()
