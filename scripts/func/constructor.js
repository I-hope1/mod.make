
function MyArray(arr) {
	let arr = this
	Array.apply(this, arr instanceof Array ? arr : [])
	this.has = i => i < arr.length
	this.put = (i, v) => i >= arr.length ? arr.push(v) : arr[i] = v
	this.append = v => arr.push(v)
	this.get = i => arr[i]
	this.remove = i => arr.splice(i, 1)[0]
	this.removeValue = v => {
		for (let i = 0; i < arr.length; i++) {
			if (arr[i] == v) return arr.splice(i, 1)[0];
		}
		return null;
	};

	this.each = this.forEach
	Object.defineProperty(this, 'toString', {
		value: function () {
			let str = []
			for (let i = 0; i < this.length; i++) {
				let item = this[i]
				let val = item instanceof Prov ? item.get() : item
				str.push(typeof val == "string" ? '"' + val + '"' : val)
			}
			return '[\n' + str.join(', ') + '\n]'
		}
	})
}
var F = function () { };
F.prototype = Array.prototype
MyArray.prototype = new F();
MyArray.prototype.constructor = MyArray;

exports.MyArray = MyArray


function MyObject(obj) {
	let map = new OrderedMap
	this.remove = k => map.remove(k)
	this.get = k => map.get(k)
	this.has = k => map.containsKey(k)
	this.getDefault = (k, def) => map.containsKey(k) ? map.get(k) : def
	this.put = (k, v) => map.put(k, v)
	this.each = function (method) {
		map.orderedKeys().each(cons(k => {
			method(k, map.get(k))
		}))
	}
	Object.defineProperty(this, 'size', { get: () => map.size })
	for (var k in obj) {
		map.put(k, obj[k])
	}
	Object.defineProperty(this, 'toString', {
		value: function () {
			let str = []
			this.each((k, v) => {
				let [key, value] = [k instanceof Prov ? k.get() : k, v instanceof Prov ? v.get() : v]
				str.push(key + ': ' + (typeof value == "string" ? '"' + value + '"' : value))
			})
			return '{\n' + str.join('\n') + '\n}'
		}
	})
}

exports.MyObject = MyObject;
/* 
function MyField(field) {
	let meta = new FieldMetadata(field)
	this.field = meta.field
	this.elementType = meta.elementType
	this.keyType = meta.keyType
}

exports.MyField = MyField; */