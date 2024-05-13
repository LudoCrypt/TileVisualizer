package net.ludocrypt.tilevis;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

	public static void main(String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		TileVisualizer tileVis = new TileVisualizer();

		config.setForegroundFPS(60);
		config.setTitle("Tile Visualizer (by LudoCrypt)");
		config.setWindowListener(new Lwjgl3WindowAdapter() {

			@Override
			public void filesDropped(String[] files) {
				tileVis.initLineOf = 0;
				tileVis.filesDropped(files);
			}

		});
		new Lwjgl3Application(tileVis, config);
	}

}
