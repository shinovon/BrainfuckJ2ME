package b;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class BCanvas extends Canvas implements CommandListener {
	
	static final Command runCmd = new Command("Run", Command.OK, 10);
	static final Command okCmd = new Command("Ok", Command.OK, 1);
	static final Command breakCmd = new Command("Break", Command.STOP, 1);
	static final Command optionsCmd = new Command("Options", Command.CANCEL, 2);
	static final Command newCmd = new Command("New", Command.CANCEL, 2);
	static final Command debugCmd = new Command("Show memory", Command.OK, 1);
	
	private Font font;
	private int fontHeight;
	private int fontWidth;
	private String[] text = new String[10];
	private int fontWidthHalf;
	protected boolean caretBlink;
	private int lines = 0;
	private int caretRow = 0;
	private int caretCol = 0;

	protected Object repaintLock = new Object();
	private Thread repainterThread;
	private Thread blinkerThread;
	
	private BInterpreter runner;
	private Thread runnerThread;

	private int width;
	private int height;
	private int rows;
	private int cols;

	private boolean console;
	private boolean noedit;
	
	private String[] log = new String[10];
	private int logCaretCol;
	private int logCaretRow;
	private int logLines;

	//boolean logInput;
	//boolean randomExt;
	
	public BCanvas() {
		addCommand(runCmd);
		addCommand(newCmd);
		setCommandListener(this);
		font = Font.getFont(0, 0, 8);
		fontHeight = font.getHeight();
		fontWidth = font.charWidth('W');
		fontWidthHalf = fontWidth >> 1;
		repainterThread = new Thread() {
			public void run() {
				while(true) {
					try {
						synchronized(repaintLock) {
							repaintLock.wait();
						}
						BCanvas.this.repaint();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		repainterThread.start();
		blinkerThread = new Thread() {
			public void run() {
				while(true) {
					try {
						caretBlink = !caretBlink;
						BCanvas.this.repaint();
						Thread.sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		blinkerThread.start();
		//logInput = false;
		//randomExt = false;
	}

	protected void paint(Graphics g) {
		width = getWidth();
		height = getHeight();
		rows = height / fontHeight;
		cols = width / fontWidth;
		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());
		if(console) {
			int ox = 0;
			if(logCaretCol >= cols) {
				ox = -(logCaretCol - cols + 1) * fontWidth;
			}
			g.translate(ox, 0);
			int cr = 0;
			if(logCaretRow >= rows) {
				cr = logCaretRow - rows + 1;
			}
			if(caretBlink) {
				g.setColor(0x787878);
				g.fillRect(logCaretCol * fontWidth, (logCaretRow-cr) * fontHeight, fontWidth, fontHeight);
			}
			g.setColor(-1);
			String[] arr = log;
			int x = 0;
			int y = 0;
			for(int row = cr; row < arr.length; row++) {
				if(y > width) break;
				x = 0;
				String s = arr[row];
				if(s == null) {
					y += fontHeight;
					continue;
				}
				for(int col = 0; col < s.length(); col++) {
					if(x >= width - ox) {
						break;
					}
					g.drawChar(s.charAt(col), x + fontWidthHalf, y, Graphics.TOP | Graphics.HCENTER);
					x += fontWidth;
				}
				y += fontHeight;
			}
		} else {
			int ox = 0;
			if(caretCol >= cols) {
				ox = -(caretCol - cols + 1) * fontWidth;
			}
			g.translate(ox, 0);
			int cr = 0;
			if(caretRow >= rows) {
				cr = caretRow - rows + 1;
			}
			if(caretBlink) {
				g.setColor(0x787878);
				g.fillRect(caretCol * fontWidth, (caretRow-cr) * fontHeight, fontWidth, fontHeight);
			}
			g.setColor(-1);
			String[] arr = text;
			int x = 0;
			int y = 0;
			for(int row = cr; row < arr.length; row++) {
				if(y > width) break;
				x = 0;
				String s = arr[row];
				if(s == null) {
					y += fontHeight;
					continue;
				}
				int l = s.length();
				for(int col = 0; col < l; col++) {
					if(x >= width - ox) break;
					char c = s.charAt(col);
					if(c != ' ') g.drawChar(c, x + fontWidthHalf, y, Graphics.TOP | Graphics.HCENTER);
					x += fontWidth;
				}
				y += fontHeight;
			}
		}
	}
	
	protected void keyPressed(int key) {
		switch(key) {
		case -1:
		case -2:
		case -3:
		case -4:
			navigate(key);
			break;
		case 8:
		case Canvas.KEY_POUND:
			clear();
			break;
		case Canvas.KEY_NUM1:
			type('<');
			break;
		case Canvas.KEY_NUM2:
			type('>');
			break;
		case Canvas.KEY_NUM3:
			type('+');
			break;
		case Canvas.KEY_NUM4:
			type('[');
			break;
		case Canvas.KEY_NUM5:
			type(']');
			break;
		case Canvas.KEY_NUM6:
			type('-');
			break;
		case Canvas.KEY_NUM7:
			type(',');
			break;
		case Canvas.KEY_NUM8:
			type('.');
			break;
		case Canvas.KEY_NUM9:
		case 13:
			type('\n');
			break;
		case 9:
			type(' ');
		case 32:
		case Canvas.KEY_NUM0:
			type(' ');
			break;
		case Canvas.KEY_STAR:
			comment();
			break;
		default:
			if(key >= 32) {
				type((char) key);
			}
			break;
		}
	}
	
	private void navigate(int key) {
		if(console) {
			switch(key) {
			case -1:
				logCaretRow --;
				if(logCaretRow < 0) logCaretRow = 0;
				break;
			case -2:
				logCaretRow ++;
				break;
			case -3:
				logCaretCol --;
				if(logCaretCol < 0) {
					logCaretCol = 0;
				}
				break;
			case -4:
				logCaretCol ++;
				break;
			}
		} else {
			switch(key) {
			case -1:
				caretRow --;
				if(caretRow < 0) caretRow = 0;
				if(caretCol >= text[caretRow].length()) caretCol = text[caretRow].length();
				break;
			case -2:
				caretRow ++;
				if(caretRow > lines) caretRow = lines;
				if(caretCol >= text[caretRow].length()) caretCol = text[caretRow].length();
				break;
			case -3:
				caretCol --;
				if(caretCol < 0) {
					if(caretRow == 0) {
						caretCol = 0;
					} else {
						caretRow --;
						caretCol = text[caretRow].length();
					}
				}
				break;
			case -4:
				caretCol ++;
				if(caretCol > text[caretRow].length()) {
					if(caretRow == lines) {
						caretCol = text[caretRow].length();
					} else {
						caretRow ++;
						caretCol = 0;
					}
				}
				break;
			}
		}
		caretBlink = true;
		callRepaint();
	}

	private void comment() {
		TextBox textBox = new TextBox("", "", 200, TextField.ANY);
		textBox.setTitle("Append");
		textBox.addCommand(okCmd);
		textBox.setCommandListener(this);
		B.display.setCurrent(textBox);
		callRepaint();
	}

	private void type(char c) {
		if(console) {
			runner = null;
			console = false;
			addCommand(runCmd);
			addCommand(newCmd);
			removeCommand(breakCmd);
			removeCommand(debugCmd);
			callRepaint();
			return;
		}
		if(noedit) return;
		if(text[caretRow] == null) text[caretRow] = "";
		if(c == '\n') {
			// expand
			if(lines + 1 >= text.length) {
				String[] tmp = text;
				text = new String[text.length + 10];
				System.arraycopy(tmp, 0, text, 0, tmp.length);
			}
			
			System.arraycopy(text, caretRow, text, caretRow+1, text.length-caretRow-1);
			if(caretCol == 0) {
				text[caretRow] = "";
			} else if(caretCol == text[caretRow].length()) {
				text[caretRow+1] = "";
			} else {
				String s = text[caretRow];
				text[caretRow] = s.substring(0, caretCol);
				text[caretRow + 1] = s.substring(caretCol);
			}
			caretCol = 0;
			lines ++;
			caretRow ++;
		} else {
			if(caretCol == text[caretRow].length()) {
				text[caretRow] += c;
			} else if(caretCol == 0) {
				text[caretRow] = c + text[caretRow];
			} else {
				text[caretRow] = text[caretRow].substring(0, caretCol) + c + text[caretRow].substring(caretCol);
			}
			caretCol ++;
		}
		if(text[caretRow] == null) text[caretRow] = "";
		callRepaint();
	}

	private void clear() {
		if(console) return;
		if(text[caretRow] == null) text[caretRow] = "";
		if(caretCol == 0) {
			if(lines > 0) {
				if(text[caretRow].length() > 0) {
					caretCol = text[caretRow-1].length();
					text[caretRow-1] += text[caretRow];
					text[caretRow] = "";
					System.arraycopy(text, caretRow+1, text, caretRow, text.length-caretRow-1);
					lines --;
					caretRow --;
				} else {
					System.arraycopy(text, caretRow+1, text, caretRow, text.length-caretRow-1);
					lines --;
					caretRow --;
					caretCol = text[caretRow].length();
				}
			}
			callRepaint();
			return;
		}
		text[caretRow] = text[caretRow].substring(0, caretCol-1) + text[caretRow].substring(caretCol);
		caretCol --;
		callRepaint();
	}

	protected void keyRepeated(int key) {
		keyPressed(key);
	}
	
	protected void keyReleased(int key) {
		
	}

	public void commandAction(Command cmd, Displayable d) {
		if(cmd == runCmd) {
			if(runner != null || console) return;
			removeCommand(runCmd);
			removeCommand(newCmd);
			addCommand(breakCmd);
			log = new String[10];
			logCaretRow = 0;
			logCaretCol = 0;
			logLines = 0;
			String s = "";
			for(int i = 0; i < text.length; i++) {
				s += text[i];
				if(i != lines) s += "\n";
			}
			final String prog = s;
			console = true;
			runner = new BInterpreter(this);
			//runner.setRandomExtension(randomExt);
			runnerThread = new Thread() {
				public void run() {
					runner.run(prog);
				}
			};
			runnerThread.start();
		}
		if(cmd == okCmd) {
			synchronized(okCmd) {
				if(console) {
					if(runner == null) return;
					runner.inputBuffer += ((TextBox)d).getString();
					synchronized(runner.inputLock) {
						runner.inputLock.notify();
					}
				} else {
					char[] c = ((TextBox)d).getString().toCharArray();
					for(int i = 0; i < c.length; i++) {
						type(c[i]);
					}
				}
				B.display.setCurrent(this);
			}
		}
		if(cmd == breakCmd) {
			if(runner == null) return;
			runner.callBreak();
		}
		if(cmd == newCmd) {
			text = new String[10];
			caretCol = 0;
			caretRow = 0;
			text[0] = "";
		}
		if(cmd == debugCmd) {
			if(runner == null) return;
			runner.debugMemory();
		}
	}
	
	public void print(char c)  {
		if(log[logLines] == null) log[logLines] = "";
		switch(c) {
		case '\n':
			if(logLines + 1 >= log.length) {
				String[] tmp = log;
				log = new String[log.length + 10];
				System.arraycopy(tmp, 0, log, 0, tmp.length);
			}
			logCaretCol = 0;
			logLines ++;
			logCaretRow = logLines;
			return;
		case '\r':
		case 8:
			return;
		case 7: // bell
			try {
				B.display.vibrate(100);
			} catch (Throwable e) {
			}
			return;
		case 0:
			try {
				Thread.sleep(50);
			} catch (Throwable e) {
			}
			return;
		default:
			logCaretCol = log[logLines].length()+1;
		}
		log[logLines] += c;
		callRepaint();
	}

	public void requestInput() {
		synchronized(okCmd) {
			TextBox textBox = new TextBox("", "", 200, TextField.ANY | TextField.NON_PREDICTIVE);
			textBox.setTitle("Input buffer");
			textBox.addCommand(okCmd);
			textBox.setCommandListener(this);
			B.display.setCurrent(textBox);
		}
	}

	public void end(String s) {
		removeCommand(breakCmd);
		addCommand(debugCmd);
		print('\n');
		print('\n');
		print(s);
		print("\nPress any num key");
	}

	public void print(String s) {
		char[] c = s.toCharArray();
		for(int i = 0; i < c.length; i++) {
			print(c[i]);
		}
	}
	
	private void callRepaint() {
		synchronized(repaintLock) {
			repaintLock.notify();
		}
	}

}
