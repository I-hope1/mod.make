
exports.cont = {
	name: 'unit_spawn', get disabled(){return Vars.state.isMenu()},
	load(){
		let ui = this.ui = new BaseDialog(this.name);
		this.unit = null; // 默认单位为null
		ui.cont.table(cons(t => {
			ItemSelection.buildTable(t, Seq(Vars.content.units().toArray()), prov(() => this.unit), cons(u => this.unit = u), false);
			// 显示单位的name和localizedName
			t.table(cons(t => {
				t.label(() => this.unit != null ? this.unit.name : '').row();
				t.label(() => this.unit != null ? this.unit.localizedName : '');
			}));
		})).row();
		var amount, team;
		ui.cont.table(cons(t => {
			amount = t.table(cons(t => t.add('amount'))).get().add(new TextField).padRight(6).get();
			team = t.table(cons(t => t.add('team'))).get().add(new TextField).get();
		}));

		ui.buttons.button('清除所有单位', () => Groups.unit.clear()).size(160, 65);
		ui.buttons.check('无单位上线', boolc(b => Vars.state.rules.unitCap = Math.pow(2, 29) * b
		)).size(160, 65).row();

		ui.buttons.button('$back', Icon.left, () => ui.hide()).size(160, 65);
		var i = 0;
		ui.buttons.button('$ok', Icon.ok, () => {
			if (this.unit == null || (amount.getText() | 0) <= 0 || (team.getText() | 0) < 0 || (team.getText() | 0) >= 255) return;
			for (var i = 0, len = amount.getText() | 0; i < len; i++) {
				this.unit.spawn(Team.get(team.getText() | 0), Vars.player.x, Vars.player.y);
			}
			ui.hide()
		}).size(160, 65);
		
	},
	click(table){
		// 如果进入游戏才显示
		if(Vars.state.isGame()) this.ui.show();
	}
};