/* 
let block = new JavaAdapter(Block, {}, '假人生成')
block.buildType = prov(() => extend(Building, {
	created(){

	}
}))
 */
let unit = new JavaAdapter(UnitType, {}, '假人');
unit.localizedName = '假人';
unit.constructor = prov(() => new JavaAdapter(UnitEntity, {
	dps: [], d: 0, lastDamage: 0, responseTime: 0,
	update(){},
	draw(){
		this.super$draw()
		this.dps.push(this.d)
		this.d = 0
		this.dps.splice(0, this.dps.length - 60 / Time.delta);
		if (Time.millis() - this.responseTime > 3000) return
		let {x, y} = this
		let dps = 0
		this.dps.forEach(item => dps += item);
		let text = '上次伤害:' + (this.lastDamage != 0 ? this.lastDamage : 0) + '\ndps:' + ('' + dps).replace(/(\d+\.\d\d)\d+/, '$1');

		let color = Color.white;
        let font = Fonts.outline;
        let layout = Pools.obtain(GlyphLayout, prov(() => new GlyphLayout));
        let ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1 / 4 / Scl.scl(1));
        layout.setText(font, text);

        font.setColor(color);
		Draw.z(120)
		Draw.color(new Color(0, 0, 0, .3));
		Draw.rect(Tex.whiteui.region, x, y - 0.5, layout.width, layout.height + 3)
        font.draw(text, x - layout.width / 2, y + layout.height / 2, Align.left)
        y -= 1;

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1);
        Draw.reset();
        Pools.free(layout);
	},
	damage(amount){
		this.lastDamage = amount;
		this.d += amount;
		this.responseTime = Time.millis();
	}
}));
