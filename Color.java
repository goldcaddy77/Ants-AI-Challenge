
public class Color {
	private int red;
	private int green;
	private int blue;

	public Color() {
		red = getRandomColorVal();
		green = getRandomColorVal();
		blue = getRandomColorVal();
	}
	
	public Color(int grayval) {
		this(grayval, grayval, grayval);
	}

	public Color(int r, int g, int b) {
		red = r;
		green = g;
		blue = b;
	}
	
	public Color withPercentage(float percentage) {
		return new Color((int)(red + red * percentage), (int)(green + green * percentage), (int)(blue + blue * percentage));
	}
	
	public int red() { return red; }
	public int green() { return green; }
	public int blue() { return blue; }
	
	private int getRandomColorVal() {
		return (int)Math.floor(Math.random() * 256) + 1;
	}
}
