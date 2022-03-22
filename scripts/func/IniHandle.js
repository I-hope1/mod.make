
const IntFunc = require("func/index")

const data = IntFunc.mod.root.child("data")
const caches = {}
exports.caches = caches

function getParent(str) {
	if (str[0] + str[1] == "//") {
		// [\u4e00-\u9fa5]为中文
		return str.replace(/\/\/\s+extend\s+([\u4e00-\u9fa5]+)[^]+/m, '$1');
	}
	return ''
}

function iniParse(_map, str) {
	// [\u4e00-\u9fa5]为中文
	let all = str.split(/\n/g);
	let map = _map || new Map()
	all.forEach(row => {
		if (row == '' || /(#|\/\/)[^\n]+/.test(row)) return;
		let [key, value] = row.split(/\s+=\s+/);
		if (value == null) return;
		map.set(key, value.replace(/\\n/g, '\n'))
	})
	return map;
}

caches.framework = (() => {
	let framework = {};
	let dir = data.child("framework")
	dir.list().forEach(list => {
		let map = new Map()
		list.findAll().each(cons(f => {
			let str = f.readString()
			let parent = getParent(str)
			let obj = { value: IntFunc.toIntObject(IntFunc.hjsonParse(str)) };
			map.set(f.nameWithoutExtension(), () => {
				let func = map.get(parent)
				if (func != null && !obj.ok) {
					obj.ok = true;
					func().each((k, v) => {
						if (!obj.value.has(k))
							obj.value.put(k, v);
					})
				}
				return obj.value
			})
		}))
		let tmp = new Map()
		map.forEach((value, key) => {
			let arr = []
			tmp.set(key, value() + "")
		})
		framework[list.name()] = tmp
	})
	return framework;
})();

caches.types = (() => {
	let fi = data.child("types").child("default.ini")
	let map = new Map()
	// [\u4e00-\u9fa5]为中文
	let all = fi.readString().match(/\w+?\s*=\s*[\w\u4e00-\u9fa5]+/g) || [];
	all.forEach(type => {
		let [key, value] = type.split(/\s*=\s*/g);
		map.set(key, value)
	})
	return map
})();

caches.content = new Map()
Events.run(ClientLoadEvent, () => {
	let file = data.child("content").child(Vars.ui.language.getLocale() + ".ini");
	if (file.exists()) {
		iniParse(caches.content, file.readString())
	}
})

