package modmake;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import modmake.components.DataHandle;
import modmake.ui.UpdateData;
import modmake.ui.content.ModMakeContent;
import modmake.ui.content.SettingContent;
import modmake.ui.MyStyles;
import modmake.util.MyReflect;
import modmake.util.load.ContentSeq;
import modmake.util.load.LoadMod;

import static modmake.IntUI.frag;
import static modmake.components.DataHandle.settings;

public class ModMake extends Mod {

	public ModMake() {
		MyReflect.load();

		Events.run(ClientLoadEvent.class, () -> {
			//			if (Vars.ui != null) throw new RuntimeException("");
			//			DataHandle.load();

			DataHandle.load();

			new SettingContent();
			new ModMakeContent();

			try {
				//				LoadMod.init();
				ContentSeq.load();
			} catch (Throwable e) {
				Vars.ui.showException("加载ContentSeq出现异常", e);
			}
			try {
				LoadMod.init();
			} catch (Throwable throwable) {
				Log.err(throwable);
			}
			MyStyles.load();
			frag.load();
			if (!settings.getBool("not_show_again")) {
				Log.info("load updateData");
				new UpdateData().show();
			}
			// imgDialog.beginEditImg(Vars.tmpDirectory.child("test.png"));
			// Pixmap pixmap = Fonts.def.getRegion().texture.getTextureData().getPixmap();
			// PixmapIO.writePng(Vars.tmpDirectory.child("test.png"), new ImgEditor.MyPixmap(pixmap.pixels, pixmap.width, pixmap.height));
			// 	imgDialog.show();
			// debug
			//			imgDialog.beginEditImg(Vars.dataDirectory.child("tmp.png"));
		});
	}

	/*static {
		PartProgress circleProgress = smoothReload, haloProgress = charge;
		Color circleColor = Color.salmon, haloColor = Color.pink;
		float circleStroke = 1;
		float circleRad = 2;
		float circleY = 0;
		float circleRotSpeed = 20;
		float haloRotSpeed = 15;
		Color heatCol = Pal.heal;
		float haloY = 10;
		new PowerTurret("klcjxmz") {{
			drawer = new DrawTurret("reinforced-") {{
				parts.addAll(

						//summoning circle
						new ShapePart() {{
							progress = circleProgress;
							color = circleColor;
							circle = true;
							hollow = true;
							stroke = 0f;
							strokeTo = circleStroke;
							radius = circleRad;
							layer = Layer.effect;
							y = circleY;
						}},

						new ShapePart() {{
							progress = circleProgress;
							rotateSpeed = -circleRotSpeed;
							color = circleColor;
							sides = 4;
							hollow = true;
							stroke = 0f;
							strokeTo = circleStroke;
							radius = circleRad - 1f;
							layer = Layer.effect;
							y = circleY;
						}},

						//outer squares

						new ShapePart() {{
							progress = circleProgress;
							rotateSpeed = -circleRotSpeed;
							color = circleColor;
							sides = 4;
							hollow = true;
							stroke = 0f;
							strokeTo = circleStroke;
							radius = circleRad - 1f;
							layer = Layer.effect;
							y = circleY;
						}},

						//inner square
						new ShapePart() {{
							progress = circleProgress;
							rotateSpeed = -circleRotSpeed / 2f;
							color = circleColor;
							sides = 4;
							hollow = true;
							stroke = 0f;
							strokeTo = 2f;
							radius = 3f;
							layer = Layer.effect;
							y = circleY;
						}},

						//spikes on circle
						new HaloPart() {{
							progress = circleProgress;
							color = circleColor;
							tri = true;
							shapes = 3;
							triLength = 0f;
							triLengthTo = 5f;
							radius = 6f;
							haloRadius = circleRad;
							haloRotateSpeed = haloRotSpeed / 2f;
							shapeRotation = 180f;
							haloRotation = 180f;
							layer = Layer.effect;
							y = circleY;
						}},

						//actual turret
						new RegionPart("-mouth") {{
							heatColor = heatCol;
							heatProgress = PartProgress.warmup;

							moveY = -8f;
						}},
						new RegionPart("-end") {{
							moveY = 0f;
						}},

						new RegionPart("-front") {{
							heatColor = heatCol;
							heatProgress = PartProgress.warmup;

							mirror = true;
							moveRot = 33f;
							moveY = -4f;
							moveX = 10f;
						}},
						new RegionPart("-back") {{
							heatColor = heatCol;
							heatProgress = PartProgress.warmup;

							mirror = true;
							moveRot = 10f;
							moveX = 2f;
							moveY = 5f;
						}},

						new RegionPart("-mid") {{
							heatColor = heatCol;
							heatProgress = PartProgress.recoil;

							moveY = -9.5f;
						}},

						new ShapePart() {{
							progress = haloProgress;
							color = haloColor;
							circle = true;
							hollow = true;
							stroke = 0f;
							strokeTo = 2f;
							radius = 10f;
							layer = Layer.effect;
							y = haloY;
						}},
						new ShapePart() {{
							progress = haloProgress;
							color = haloColor;
							sides = 3;
							rotation = 90f;
							hollow = true;
							stroke = 0f;
							strokeTo = 2f;
							radius = 4f;
							layer = Layer.effect;
							y = haloY;
						}},
						new HaloPart() {{
							progress = haloProgress;
							color = haloColor;
							sides = 3;
							shapes = 3;
							hollow = true;
							stroke = 0f;
							strokeTo = 2f;
							radius = 3f;
							haloRadius = 10f + radius / 2f;
							haloRotateSpeed = haloRotSpeed;
							layer = Layer.effect;
							y = haloY;
						}},

						new HaloPart() {{
							progress = haloProgress;
							color = haloColor;
							tri = true;
							shapes = 3;
							triLength = 0f;
							triLengthTo = 10f;
							radius = 6f;
							haloRadius = 16f;
							haloRotation = 180f;
							layer = Layer.effect;
							y = haloY;
						}},
						new HaloPart() {{
							progress = haloProgress;
							color = haloColor;
							tri = true;
							shapes = 3;
							triLength = 0f;
							triLengthTo = 3f;
							radius = 6f;
							haloRadius = 16f;
							shapeRotation = 180f;
							haloRotation = 180f;
							layer = Layer.effect;
							y = haloY;
						}},

						new HaloPart() {{
							progress = haloProgress;
							color = haloColor;
							sides = 3;
							tri = true;
							shapes = 3;
							triLength = 0f;
							triLengthTo = 10f;
							shapeRotation = 180f;
							radius = 6f;
							haloRadius = 16f;
							haloRotateSpeed = -haloRotSpeed;
							haloRotation = 180f / 3f;
							layer = Layer.effect;
							y = haloY;
						}},

						new HaloPart() {{
							progress = haloProgress;
							color = haloColor;
							sides = 3;
							tri = true;
							shapes = 3;
							triLength = 0f;
							triLengthTo = 4f;
							radius = 6f;
							haloRadius = 16f;
							haloRotateSpeed = -haloRotSpeed;
							haloRotation = 180f / 3f;
							layer = Layer.effect;
							y = haloY;
						}}
				);

				Color heatCol2 = heatCol.cpy().add(0.1f, 0.1f, 0.1f).mul(1.2f);
				for (int i = 1; i < 4; i++) {
					int fi = i;
					parts.add(new RegionPart("-spine") {{
						outline = false;
						progress = PartProgress.warmup.delay(fi / 5f);
						heatProgress = PartProgress.warmup.add(p -> (Mathf.absin(3f, 0.2f) - 0.2f) * p.warmup);
						mirror = true;
						under = true;
						layerOffset = -0.3f;
						turretHeatLayer = Layer.turret - 0.2f;
						moveY = 9f;
						moveX = 1f + fi * 4f;
						moveRot = fi * 60f - 130f;

						color = Color.valueOf("bb68c3");
						heatColor = heatCol2;
						moves.add(new PartMove(PartProgress.recoil.delay(fi / 5f), 1f, 0f, 3f));
					}});
				}
			}};
			health = (int) 2E9;
			size = 4;
			consumePower(1);
			requirements(Category.turret, ItemStack.empty);

			buildType = PowerTurretBuild::new;

			shootType = new LightningBulletType() {{
				damage = 20;
				lightningLength = 25;
				collidesAir = false;
				ammoMultiplier = 1f;

				//for visual stats only.
				buildingDamageMultiplier = 0.25f;

				lightningType = new BulletType(0.0001f, 0f) {{
					lifetime = Fx.lightning.lifetime;
					hitEffect = Fx.hitLancer;
					despawnEffect = Fx.none;
					status = StatusEffects.shocked;
					statusDuration = 10f;
					hittable = false;
					lightColor = Color.white;
					collidesAir = false;
					buildingDamageMultiplier = 0.25f;
				}};
			}};

		}};


	}*/
}