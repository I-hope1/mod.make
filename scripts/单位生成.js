elements['单位生成'] = {
	load(){
		let ui = this.ui = new BaseDialog('生成单位');
		this.unit = null;
		ui.cont.table(cons(t => {
			ItemSelection.buildTable(t, Seq(Vars.content.units().toArray()), prov(() => this.unit), cons(u => this.unit = u), false);
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
		ui.buttons.button('$back', Icon.left, run(() => ui.hide())).size(160, 65);
		ui.buttons.button('$ok', Icon.ok, run(() => this.unit == null || ui.hide() || Array(amount.getText() | 0).fill(0).forEach(e => this.unit.spawn(Team.get(team.getText() | 0), Vars.player.x, Vars.player.y)))).size(160, 65);
	},
	buildConfiguration(table){
		if(Vars.state.isGame()) this.ui.show();
	}
}