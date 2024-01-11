package modmake.util.img;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;

public class MyPixmapIO {
	public static void write(Pixmap pixmap, Fi toFile) {
		//		toFile.writePng(pixmap);
		PixmapIO.writePng(toFile, pixmap);
	}
}
