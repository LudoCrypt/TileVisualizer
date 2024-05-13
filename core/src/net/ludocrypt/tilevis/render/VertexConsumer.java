package net.ludocrypt.tilevis.render;

public interface VertexConsumer {

	public void begin();

	public void end();

	public VertexConsumer vertex(float... arr);

	public void endVertex();

	default public VertexConsumer color(int r, int g, int b, int a) {
		return this.vertex(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	default public VertexConsumer color(int i) {
		return this.color(red(i), green(i), blue(i), alpha(i));
	}

	default public VertexConsumer uv(float u, float v) {
		return this.vertex(u, v);
	}

	public static int alpha(int i) {
		return i >>> 24;
	}

	public static int red(int i) {
		return i >> 16 & 0xFF;
	}

	public static int green(int i) {
		return i >> 8 & 0xFF;
	}

	public static int blue(int i) {
		return i & 0xFF;
	}

	public static int col(int i, int j, int k, int l) {
		return i << 24 | j << 16 | k << 8 | l;
	}

}
