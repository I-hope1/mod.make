package modmake.ui;

import arc.graphics.g2d.Draw;
import arc.scene.Element;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Tex;

public class DragLayout extends WidgetGroup {
	float space = Scl.scl(10f), prefWidth, prefHeight;
	Seq<Element> seq            = new Seq<>();
	int          insertPosition = 0;
	float        targetWidth;
	boolean      invalidated;
	Element      dragging;

	{
		setTransform(true);
	}

	@Override
	public void layout() {
		invalidated = true;
		float cy = 0;
		seq.clear();

		float totalHeight = getChildren().sumf(e -> e.getHeight() + space);

		height = prefHeight = totalHeight;
		width = prefWidth = 300;

		//layout everything normally
		for (int i = 0; i < getChildren().size; i++) {
			Element e = getChildren().get(i);

			//ignore the dragged element
			if (dragging == e) continue;

			e.setSize(width, e.getPrefHeight());
			e.setPosition(0, height - cy, Align.topLeft);
			// ((StatementElem) e).updateAddress(i);

			cy += e.getPrefHeight() + space;
			seq.add(e);
		}

		//insert the dragged element if necessary
		if (dragging != null) {
			//find real position of dragged element top
			float realY = dragging.getY(Align.top) + dragging.translation.y;

			insertPosition = 0;

			for (int i = 0; i < seq.size; i++) {
				Element cur = seq.get(i);
				//find fit point
				if (realY < cur.y && (i == seq.size - 1 || realY > seq.get(i + 1).y)) {
					insertPosition = i + 1;
					break;
				}
			}

			float shiftAmount = dragging.getHeight() + space;

			//shift elements below insertion point down
			for (int i = insertPosition; i < seq.size; i++) {
				seq.get(i).y -= shiftAmount;
			}
		}

		invalidateHierarchy();

		if (parent != null && parent instanceof Table) {
			setCullingArea(parent.getCullingArea());
		}
	}

	@Override
	public float getPrefWidth() {
		return prefWidth;
	}

	@Override
	public float getPrefHeight() {
		return prefHeight;
	}

	@Override
	public void draw() {
		Draw.alpha(parentAlpha);

		//draw selection box indicating placement position
		if (dragging != null && insertPosition <= seq.size) {
			float shiftAmount = dragging.getHeight();
			float lastX       = x;
			float lastY       = insertPosition == 0 ? height + y : seq.get(insertPosition - 1).y + y - space;

			Tex.pane.draw(lastX, lastY - shiftAmount, width, dragging.getHeight());
		}

		if (invalidated) {
			children.each(c -> c.cullable = false);
		}

		super.draw();

		if (invalidated) {
			children.each(c -> c.cullable = true);
			invalidated = false;
		}
	}

	void finishLayout() {
		if (dragging != null) {
			//reset translation first
			for (Element child : getChildren()) {
				child.setTranslation(0, 0);
			}
			clearChildren();

			//reorder things
			for (int i = 0; i <= insertPosition - 1 && i < seq.size; i++) {
				addChild(seq.get(i));
			}

			addChild(dragging);

			for (int i = insertPosition; i < seq.size; i++) {
				addChild(seq.get(i));
			}

			dragging = null;
		}

		layout();
	}
	public DragLayout() {
	}
}
