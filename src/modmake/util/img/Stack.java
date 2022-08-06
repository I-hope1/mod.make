package modmake.util.img;

import arc.util.Time;
import modmake.components.DataHandle;
import modmake.ui.img.ImgEditor;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static modmake.IntUI.view;

public class Stack {
	private final ImgEditor imgEditor;
	public static int maxSize = DataHandle.settings.getInt("max_stack_buffer_size", 15);
	public ByteBuffer tmp = null;
	public ArrayList<ByteBuffer> undoes = new ArrayList<>();
	public ArrayList<ByteBuffer> redoes = new ArrayList<>();

	public Stack(ImgEditor imgEditor) {
		this.imgEditor = imgEditor;
	}

	public void addUndo(ByteBuffer pixels) {
		undoes.add(pixels);
		while (undoes.size() > maxSize) {
			undoes.remove(0).clear();
		}
	}

	public void addRedo(ByteBuffer pixels) {
		redoes.add(pixels);
		while (redoes.size() > maxSize) {
			redoes.remove(0).clear();
		}
	}

	public void clear() {
		Time.runTask(1, () -> {
			undoes.forEach(ByteBuffer::clear);
			redoes.forEach(ByteBuffer::clear);
		});
		undoes.clear();
		redoes.clear();
	}

	public void undo() {
		if (canUndo()) {
			ByteBuffer pixmap = undoes.remove(undoes.size() - 1);
			addRedo(setPixmap(pixmap));
		}
	}

	public boolean canUndo() {
		return undoes.size() > 0;
	}

	public void redo() {
		if (canRedo()) {
			ByteBuffer pixels = redoes.remove(redoes.size() - 1);
			addUndo(setPixmap(pixels));
		}
	}

	public ByteBuffer setPixmap(ByteBuffer newPixels) {
		ByteBuffer pixels = imgEditor.pixmap().pixels;

//			view.cont.texture.draw(pixmap());
		imgEditor.pixmap().pixels = newPixels;
		view.rebuildCont();
		return pixels;
	}

	public boolean canRedo() {
		return redoes.size() > 0;
	}
}
