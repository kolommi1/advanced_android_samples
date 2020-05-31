package cz.uhk.advanced_android_samples.object_visualization.oglutils;

import android.opengl.GLES20;
import android.util.Log;


public class OGLRenderTarget {
	protected final int width, height, count;
	protected final int[] drawBuffers;
	protected final int[] frameBuffer = new int[1];
	protected final OGLTexture2D[] colorBuffers;
	protected final OGLTexture2D depthBuffer;
	private static final String TAG = "OGLRenderTarget";
	
	public OGLRenderTarget( int width, int height) {
		this( width, height, 1);
	}

	public OGLRenderTarget( int width, int height, int count) {
		this.width = width;
		this.height = height;
		this.count = count;
		this.colorBuffers = new OGLTexture2D[count];
		this.drawBuffers = new int[count];
		for (int i = 0; i < count; i++) {
			colorBuffers[i] = new OGLTexture2D( width, height, GLES20.GL_RGBA,
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			drawBuffers[i] = GLES20.GL_COLOR_ATTACHMENT0 + i;
		}

		this.depthBuffer = new OGLTexture2D(width, height,
				GLES20.GL_DEPTH_COMPONENT, GLES20.GL_DEPTH_COMPONENT,
				GLES20.GL_UNSIGNED_INT, null);

		GLES20.glGenFramebuffers(1, frameBuffer, 0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
		for (int i = 0; i < count; i++)
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0   + i, GLES20.GL_TEXTURE_2D,
					colorBuffers[i].getTextureId(), 0);

		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D,
				depthBuffer.getTextureId(), 0);

		if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			Log.i(TAG,"There is a problem with the FBO");
		}
	}

	public void bind() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
		GLES20.glViewport(0, 0, width, height);
	}

	public void bindColorTexture(int shaderProgram, String name, int slot) {
		bindColorTexture(shaderProgram, name, slot, 0);
	}

	public void bindColorTexture(int shaderProgram, String name, int slot, int bufferIndex) {
		colorBuffers[bufferIndex].bind(shaderProgram, name, slot);
	}

	public void bindDepthTexture(int shaderProgram, String name, int slot) {
		depthBuffer.bind(shaderProgram, name, slot);
	}

	public OGLTexture2D getColorTexture() {
		return getColorTexture(0);
	}

	public OGLTexture2D getColorTexture(int bufferIndex) {
		return colorBuffers[bufferIndex];
	}

	public OGLTexture2D getDepthTexture() {
		return depthBuffer;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}


}
