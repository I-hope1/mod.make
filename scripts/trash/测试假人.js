// 没用的
let unit = new JavaAdapter(UnitType, {
	dps:[],
	update(u){
		if(this.table != null) return;
		let table = this.table = new Table;
		table.setFillParent(true);
		table.touchable = Touchable.disabled;
		table.update(run(() => {
			if(Vars.state.isMenu()){
				table.remove();
			}
		}));
		table.align(Align.center).table(Styles.black3).left().update(t => {
			let v = Core.camera.project(this.x, this.y);
			t.setPosition(v.x, v.y, Align.center);
			t.clearChildren();
			this.lastDamage && t.add('上次伤害:' + this.lastDamage).row();
			t.add('dps:' + eval(this.dps.join('+')) / 60);
		});
		table.act(0);
		Core.scene.root.addChildAt(0, table);
	},
	damage(amount){
		this.lastDamage = amount;
		this.dps.push(amount);
		this.splice(0, this.dps.length - 60);
	}
}, '测试假人');
unit.localizedName = '测试假人';
unit.constructor = prov(() => new JavaAdapter(Unit, {}));
