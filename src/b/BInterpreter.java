package b;

import java.util.Random;

public class BInterpreter {
	
	public static final int memory_size = 30000; 
	
	// memory
	private byte[] memory;
	// memory index
	private int i;
	// program
	private char[] prog;
	// program index
	private int prog_index;
	
	private int brc;
	// stop switch
	private boolean stop;
	
	String inputBuffer;
	Object inputLock;

	BCanvas editor;

	private Random rng;
	private boolean randomExt;
	
	public BInterpreter(BCanvas editor) {
		this.editor = editor;
		memory = new byte[memory_size];
		i = 0;
		inputLock = new Object();
		rng = new Random();
	}

	public void run(String s) {
		char[] chars = s.toCharArray();
		s = null;
		// Simplify (remove all unnecessary characters)
		char[] tmp = new char[chars.length];
		int len = 0;
		for(int i = 0; i < chars.length; i++) {
			char c = chars[i];
			switch(c) {
			case '>':
			case '<':
			case '+':
			case '-':
			case '.':
			case ',':
			case '[':
			case ']':
			case '?':
				if(c != '?' || randomExt)
					tmp[len++] = c;
				break;
			}
		}
		prog = new char[len];
		System.arraycopy(tmp, 0, prog, 0, len);
		tmp = null;
		int ind = 0;
		// Interpretator
		try {
			for(prog_index = 0; prog_index < prog.length && !stop; prog_index++) {
				switch(prog[ind = prog_index]) {
				case '>':
					if(i++ == memory.length - 1) i = 0;
					continue;
				case '<':
					if(i-- == 0) i = memory.length - 1;
					continue;
				case '+': 
					memory[i]++;
					continue;
				case '-': 
					memory[i]--;
					continue;
				case '.':
					print((char) (memory[i] & 0xff));
					break;
				case ',':
					requestInput();
					continue;
				case '[': 
					if (memory[i] != 0) continue;
					++brc;
					while (brc > 0) {
						++prog_index;
						if (prog[prog_index] == '[') ++brc;
						if (prog[prog_index] == ']') --brc;
					}
					continue;
				case ']':
					if (memory[i] == 0) continue;
					if (prog[prog_index] == ']') brc++;
					while (brc > 0) {
						--prog_index;
						if (prog[prog_index] == '[') brc--;
						if (prog[prog_index] == ']') brc++;
					}
					--prog_index;
					continue;
				case '?':
					if(randomExt) memory[i] = (byte) rng.nextInt(256);
					continue;
				}
			}
			if(stop) {
				editor.end("Break at " + prog_index);
				return;
			}
			editor.end("Done");
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			int i = 0;
			try {
				i = Integer.parseInt(e.toString());
			} catch (Exception e2) {
			}
			error("Unclosed brackets", i, ind);
			editor.end("Done");
		}
	}
	
	public void setRandomExtension(boolean enable) {
		randomExt = enable;
	}
	
	private void print(char c) {
		editor.print(c);
	}

	private void requestInput() {
		if(inputBuffer != null && inputBuffer.length() > 0) {
			int t = inputBuffer.charAt(0);
			inputBuffer = inputBuffer.substring(1);
			if(t > 255) t = 0;
			memory[i] = (byte) t;
			return;
		}
		inputBuffer = "";
		try {
			editor.requestInput();
			synchronized(inputLock) {
				inputLock.wait();
			}
		} catch (Exception e) {
		}
		String s = inputBuffer;
		int t = 0;
		if(s.length() > 0) {
			t = s.charAt(0);
			if(t > 255) t = 0;
		}
		if(s.length() > 1) {
			inputBuffer = s.substring(1);
		} else {
			inputBuffer = "";
		}
		memory[i] = (byte) t;
	}

	private void error(String e, int j, int k) {
		editor.print("ERROR " + e + "  at " + k + "\n");
		stop = true;
		String s = new String(prog);
		int g = 24;
		int v = k - g / 2;
		if(s.length() > g && v > 0) {
			s = "..." + s.substring(v);
			k -= v - 3;
		}
		
		if(s.length() > g) {
			s = s.substring(0, g) + "...";
		}
		
		editor.print(s + "\n");
		for(; k > 0; k--) editor.print(" ");
		editor.print("^\n");
	}
	
	void callBreak() {
		stop = true;
	}

	public void debugMemory() {
		int len = 0;
		for(int i = memory.length - 1; i >= 0; i--) {
			if(memory[i] != 0) {
				len = i + 1;
				break;
			}
		}
		editor.print("\n== Memory View start ==\n");
		final int w = 16;
		if(len < w) len = w;
		int l = memory.length;
		int i = 0;
		int h = len / w + 1;
		for(int y = 0; y < h && i < l; y++) {
			editor.print(hex(i, 4) + ": ");
			byte[] tmp = new byte[w];
			int x = 0;
			boolean b = false;
			for(; x < w && i < l; x++) {
				if(memory[i] != 0) {
					b = true;
					break;
				}
			}
			if(b) {
				x = 0;
				for(; x < w && i < l; x++) {
					if(x != 0) editor.print(" ");
					editor.print(hex((tmp[x] = memory[i]) & 0xff, 2));
					i++;
				}
				if(i == l) {
					for(; x < w; x++) {
						if(x != 0) editor.print(" ");
						editor.print("00");
					}
				}
				editor.print(" ");
				for(int j = 0; j < tmp.length; j++) {
					char c = (char) (tmp[j] & 0xff);
					if(c < 32) c = '.';
					editor.print(c);
				}
				editor.print("\n");
			}
		}
		editor.print("==  Memory View end  ==\n");
	}

	private static String hex(int i, int j) {
		String s = Integer.toHexString(i);
		while(s.length() < j) s = "0" + s;
		return s;
	}

}
