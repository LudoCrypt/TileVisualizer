package net.ludocrypt.tilevis.render;

import java.util.Arrays;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;

public class MeshConsumer implements VertexConsumer {

	private final Mesh mesh;

	private final boolean quads;

	private final int vertexSize;
	private final int[] attributeSizes;

	private boolean building = false;

	private int batch = 0;
	private int idx = 0;
	private int atb = 0;

	private float[] vertices;
	private short[] indices;

	private final Attribute[] attributes;

	public MeshConsumer(Mesh mesh, boolean quads) {
		this.mesh = mesh;
		this.quads = quads;

		int vertComp = 0;

		attributes = new Attribute[mesh.getVertexAttributes().size()];
		attributeSizes = new int[attributes.length];

		for (int i = 0; i < attributes.length; i++) {
			attributes[i] = new Attribute(mesh.getVertexAttributes().get(i));
			attributeSizes[i] = vertComp;
			vertComp += attributes[i].size;
		}

		vertexSize = vertComp;

	}

	@Override
	public void begin() {

		if (building) {
			throw new UnsupportedOperationException("Already Building");
		}

		batch = 0;
		idx = 0;

		vertices = new float[mesh.getMaxVertices() * vertexSize];
		indices = new short[mesh.getMaxIndices()];

		for (Attribute attribute : attributes) {
			attribute.reset();
		}

		building = true;
	}

	@Override
	public void end() {

		if (!building) {
			throw new UnsupportedOperationException("Not Building");
		}

		batch = 0;
		idx = 0;

		building = false;

		for (Attribute attribute : attributes) {
			attribute.reset();
		}

		mesh.setVertices(vertices);
		mesh.setIndices(indices);
	}

	@Override
	public VertexConsumer vertex(float... arr) {

		if (!building) {
			throw new UnsupportedOperationException("Not building");
		}

		if (atb > attributes.length) {
			throw new UnsupportedOperationException("Exceeding required elements");
		}

		if (attributes[atb].set) {
			throw new UnsupportedOperationException("Already set element");
		}

		attributes[atb].setBuf(arr);

		atb++;

		return this;
	}

	@Override
	public void endVertex() {

		if (!building) {
			throw new UnsupportedOperationException("Not Built");
		}

		for (Attribute attribute : attributes) {

			if (!attribute.set) {
				throw new UnsupportedOperationException("Not all elements of the vertex set");
			}

		}

		for (int i = 0; i < this.attributes.length; i++) {
			this.attributes[i].match(vertices, this.attributeSizes[i] + idx * vertexSize);
		}

		if (idx % (quads ? 4 : 3) == 0) {
			int scl = (quads ? 6 : 3);
			int scl2 = (quads ? 4 : 3);

			indices[batch * scl] = (short) (batch * scl2);
			indices[batch * scl + 1] = (short) (batch * scl2 + 1);
			indices[batch * scl + 2] = (short) (batch * scl2 + 2);

			if (quads) {
				indices[batch * scl + 3] = (short) (batch * scl2 + 2);
				indices[batch * scl + 4] = (short) (batch * scl2 + 3);
				indices[batch * scl + 5] = (short) (batch * scl2);
			}

			batch++;
		}

		idx++;

		atb = 0;

		for (Attribute attribute : this.attributes) {
			attribute.reset();
		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Vertices: ");
		builder.append(Arrays.toString(this.vertices));
		builder.append(System.lineSeparator());
		builder.append("Indices: ");
		builder.append(Arrays.toString(this.indices));

		return builder.toString();
	}

	private class Attribute {

		protected final int size;

		protected boolean set;
		protected float[] buf;

		public Attribute(VertexAttribute attrb) {
			size = attrb.numComponents;
			this.reset();
		}

		public void reset() {
			set = false;
			buf = new float[size];
		}

		public void setBuf(float... buf) {

			if (set) {
				throw new UnsupportedOperationException("Already set attribute");
			}

			this.buf = Arrays.copyOf(buf, buf.length);
			set = true;
		}

		public void match(float[] arr, int offset) {

			for (int i = 0; i < buf.length; i++) {
				arr[offset + i] = buf[i];
			}

		}

	}

}
