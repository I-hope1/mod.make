
var useable = require('testFi').useable;
exports.cont = {
	name:'more', show: false,

	all: ['lastlog', 'unit_spawn', 'showcrashes', 'select'].map(str => {
		if (!Core.settings.get(modName + '-load-' + str, true)) return
		try {
			var cont = require('content/others/' + str).cont;
			return !cont.needFi || useable ? cont : null;
		} catch (e) {
			Log.err('' + e);
		}
		return null;
	}),

	load(){
		let t = this.ui = new Table;
		this.all.forEach(cont => {
			if (cont == null) return;
			if (cont.load instanceof Function) cont.load();
			cont.name = Core.bundle.get(cont.name, cont.name)

			t.button(cont.name, run(() => cont.click && cont.click())).size(120, 40).disabled(boolf(() => !!cont.disabled)).row();
		})
	},
	buildConfiguration(table){
		this.show = true//!this.show;
		// this.btn.setChecked(this.show);
		if (this.show) table.add(this.ui);
	}
}