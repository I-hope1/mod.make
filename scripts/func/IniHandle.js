
const { MyObject, MyArray } = require('func/constructor')

const data = Vars.mods.locateMod(modName).root.child("data")
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
			let obj = { value: toIntObject(hjsonParse(str)) };
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

caches.settings = (() => {
	const settings = Vars.dataDirectory.child("mods(I hope...)").child("settings.txt")
	if (!settings.exists()) settings.writeString('')
	let map = new Map()
	let all = settings.readString().match(/[\w-]+?\s*:\s*\w+/g) || [];
	all.forEach(type => {
		let [key, value] = type.split(/\s*:\s*/g);
		map.set(key, value)
	})
	return {
		get(k) {
			let v = map.get(k)
			return v == null ? null : v == "true"
		},
		set(k, v) {
			map.set(k, v)
			let str = []
			for (let [key, value] of map) {
				str.push(key + ":" + value);
			}
			settings.writeString(str.join('\n'))
		}
	}
})()

/* hjson解析 (使用arc的JsonReader) */
function hjsonParse(str) {
	if (typeof str !== 'string') return str;
	if (str.replace(/^\s+/, '')[0] != '{') str = '{\n' + str + '\n}'
	try {
		return (new JsonReader).parse(str)
	} catch (err) {
		Log.err(err);
		return null;
	}
}

function toIntObject(value) {
	let obj2 = new MyObject(), arr = []
	let output = obj2
	while (true) {
		for (let child = value.child; child != null; child = child.next) {
			let result = (() => {
				if (child.isArray()) {
					let array = new MyArray()
					arr.push(child, array);
					return array
				}
				if (child.isObject()) {
					let obj = new MyObject()
					arr.push(child, obj);
					return obj
				}

				let value = child.asString()
				if (child.isNumber()) value *= 1
				if (child.isBoolean()) value = value == 'true'
				return value
			})()
			if (obj2 instanceof Array) obj2.push(result)
			else obj2.put(child.name, result)
		}
		if (arr.length == 0) break
		value = arr.shift()
		obj2 = arr.shift()
	}
	// Log.info(output + "")
	return output
}

exports.hjsonParse = hjsonParse;
exports.toIntObject = toIntObject;