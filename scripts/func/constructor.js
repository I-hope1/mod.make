
function array(arr) {
	Array.apply(this)
	arr instanceof Array && arr.forEach(item => this.push(item))
	this.put = (i, v) => this.splice(i, 0, v)
	this.get = i => this[i]
	this.remove = i => this.splice(i, 1)
	Object.defineProperty(this, 'toString', {
		value: function () {
			let str = []
			this.forEach(item => str.push(item instanceof Prov ? item.get() : item))
			return '[ ' + str.join(', ') + ' ]'
		}
	})
}
var F = function () { };
F.prototype = Array.prototype
array.prototype = new F();
array.prototype.constructor = array;

exports.Array = array


function object(obj) {
	Object.apply(this)
	let map = new ObjectMap
	this.remove = k => map.remove(k)
	this.get = k => map.get(k)
	this.has = k => map.containsKey(k)
	let tmp
	this.getDefault = (k, def) => map.get(k, prov(() => def))
	this.put = (k, v) => map.put(k, v)
	this.each = function(method){
		map.each(new Cons2({get: method}))
	}
	Object.defineProperty(this, 'size', { get: () => map.size })
	for (var k in obj) {
		map.put(k, obj[k])
	}
	Object.defineProperty(this, 'toString', {
		value: function () {
			let str = []
			this.each((k, v) => str.push(k + ': ' + (v instanceof Prov ? v.get() : v)))
			return str.join('\n')
		}
	})
}
var F = function () { };
F.prototype = object.prototype
object.prototype = new F();
object.prototype.constructor = object;

exports.Object = object;