package b;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class B extends MIDlet {

	static Display display;
	
	private boolean started;

	protected void destroyApp(boolean b) {
	}

	protected void pauseApp() {
	}

	protected void startApp() {
		if(started) return;
		started = true;
		display = Display.getDisplay(this);
		display.setCurrent(new BCanvas());
	}

}
