package cz.uhk.advanced_android_samples.object_visualization.oglutils;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;


public class OGLBuffers {
	static public class Attrib {
		String name;
		int dimension;
		boolean normalize = false;
		int offset = -1;

		public Attrib(String name, int dimension) {
			this.name = name;
			this.dimension = dimension;
		}

		public Attrib(String name, int dimension, int offsetInFloats) {
			this.name = name;
			this.dimension = dimension;
			this.offset = 4 * offsetInFloats;
		}

		public Attrib(String name, int dimension, boolean normalize) {
			this.name = name;
			this.dimension = dimension;
			this.normalize = normalize;
		}

		public Attrib(String name, int dimension, boolean normalize, int offsetInFloats) {
			this.name = name;
			this.dimension = dimension;
			this.normalize = normalize;
			this.offset = 4 * offsetInFloats;
		}
	}

	protected class VertexBuffer {
		int id, stride;
		Attrib[] attributes;

		public VertexBuffer(int id, int stride, Attrib[] attributes) {
			this.id = id;
			this.stride = stride;
			this.attributes = attributes;
		}
	}

	protected List<VertexBuffer> vertexBuffers = new ArrayList<>();
	protected List<Integer> attribArrays = null;
	protected int[] indexBuffer;
	protected int indexCount = -1;
	protected int vertexCount = -1;
	private static final String TAG = "OGLbuffers";

	public OGLBuffers(float[] vertexData, Attrib[] attributes, short[] indexData) {
		addVertexBuffer(vertexData, attributes);
		if (indexData != null)
			setIndexBuffer(indexData);
	}

	public OGLBuffers( float[] vertexData, int floatsPerVertex, Attrib[] attributes, short[] indexData) {
		addVertexBuffer(vertexData, floatsPerVertex, attributes);
		if (indexData != null)
			setIndexBuffer(indexData);
	}

	public void addVertexBuffer(float[] data, Attrib[] attributes) {
		if (attributes == null || attributes.length == 0)
			return;

		int floatsPerVertex = 0;
		for (int i = 0; i < attributes.length; i++)
			floatsPerVertex += attributes[i].dimension;

		addVertexBuffer(data, floatsPerVertex, attributes);
	}

	public void addVertexBuffer(float[] data, int floatsPerVertex, Attrib[] attributes) {
		int[] bufferID = new int[1];
		FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer.put(data);
		buffer.position(0);

		GLES20.glGenBuffers(1, bufferID, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferID[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, buffer, GLES20.GL_STATIC_DRAW);

		if (data.length % floatsPerVertex != 0)
			throw new RuntimeException(
					"The total number of floats is incongruent with the number of floats per vertex.");
		if (vertexCount < 0)
			vertexCount = data.length / floatsPerVertex;
		else if (vertexCount != data.length / floatsPerVertex)
			Log.w(TAG,"Warning: GLBuffers.addVertexBuffer: vertex count differs from the first one.");

		vertexBuffers.add(new VertexBuffer(bufferID[0], floatsPerVertex * 4, attributes));
	}

	public void setIndexBuffer(short[] data) {
		indexBuffer = new int[1];
		indexCount = data.length;
		ShortBuffer buffer = ByteBuffer.allocateDirect(data.length * 2)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		buffer.put(data);
		buffer.position(0);

		GLES20.glGenBuffers(1, indexBuffer, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, data.length * 2, buffer, GLES20.GL_STATIC_DRAW);
	}

	public void bind(int shaderProgram) {
		if (attribArrays != null)
			for (Integer attrib : attribArrays)
				GLES20.glDisableVertexAttribArray(attrib);
		attribArrays = new ArrayList<>();
		for (VertexBuffer vb : vertexBuffers) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vb.id);
			int offset = 0;
			for (int j = 0; j < vb.attributes.length; j++) {
				int location = GLES20.glGetAttribLocation(shaderProgram, vb.attributes[j].name);
				if (location >= 0) {// due to optimization GLSL on a graphic card
					attribArrays.add(location);
					GLES20.glEnableVertexAttribArray(location);
					GLES20.glVertexAttribPointer(location, vb.attributes[j].dimension, GLES20.GL_FLOAT,
							vb.attributes[j].normalize, vb.stride,
							vb.attributes[j].offset < 0 ? offset : vb.attributes[j].offset);
				}
				offset += 4 * vb.attributes[j].dimension;
			}
		}

		if (indexBuffer != null)
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);

	}

	public void unbind() {
		if (attribArrays != null) {
			for (Integer attrib : attribArrays)
				GLES20.glDisableVertexAttribArray(attrib);
			attribArrays = null;
		}
		//unbind from the buffers
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public void draw(int topology, int shaderProgram) {
		// gl.glUseProgram(shaderProgram);
		bind(shaderProgram);
		if (indexBuffer == null) {
			GLES20.glDrawArrays(topology, 0, vertexCount);
		} else {
			GLES20.glDrawElements(topology, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
		}
		unbind();
	}

	public void draw(int topology, int shaderProgram, int count) {
		draw(topology, shaderProgram, count, 0);
	}

	public void draw(int topology, int shaderProgram, int count, int start) {
		// gl.glUseProgram(shaderProgram);
		bind(shaderProgram);
		if (indexBuffer == null) {
			GLES20.glDrawArrays(topology, start, count);
		} else {
			GLES20.glDrawElements(topology, count, GLES20.GL_UNSIGNED_SHORT, start * 2);
		}
		unbind();
	}

}
