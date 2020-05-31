package cz.uhk.advanced_android_samples.object_visualization.oglutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import cz.uhk.advanced_android_samples.object_visualization.transforms.Mat4Scale;
import cz.uhk.advanced_android_samples.object_visualization.transforms.Mat4Transl;

public class OGLTextureCube implements OGLTexture {

    private final int[] textureID = new int[1];

    private class TargetSize {
        private final int width, height;
        public TargetSize(int width, int height) {
            this.width = width; this.height = height;
        }
        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }

    }

    private final TargetSize[] targetSize;
    public static final String[] SUFFICES_POS_NEG = { "posx", "negx", "posy", "negy", "posz", "negz" };
    public static final String[] SUFFICES_POS_NEG_FLIP_Y = { "posx", "negx", "negy", "posy", "posz", "negz" };
    public static final String[] SUFFICES_POSITIVE_NEGATIVE = { "positive_x", "negative_x", "positive_y", "negative_y", "positive_z", "negative_z" };
    public static final String[] SUFFICES_POSITIVE_NEGATIVE_FLIP_Y = { "positive_x", "negative_x", "negative_y", "positive_y", "positive_z", "negative_z" };
    public static final String[] SUFFICES_RIGHT_LEFT = { "right", "left", "bottom", "top", "front", "back" };
    public static final String[] SUFFICES_RIGHT_LEFT_FLIP_Y  = { "right", "left", "top", "bottom", "front", "back" };
    private static final int[] TARGETS = { GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
            GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
            GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
            GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
            GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
            GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z };

    public static class Viewer extends OGLTexture2D.Viewer {
        private static final String SHADER_VERT_SRC =
                "attribute vec2 inPosition;"+
                "attribute vec2 inTexCoord;"+
                "uniform mat4 matTrans;"+
                "varying vec2 texCoord;"+
                "void main() {"+
                "	gl_Position = matTrans * vec4(inPosition , 0.0, 1.0);"+
                "   texCoord = inTexCoord;"+
                "}"
        ;

        private static final String SHADER_FRAG_SRC =
                "precision mediump float;"+
                "varying vec2 texCoord;"+
                "uniform samplerCube drawTexture;"+
                "void main() {"+
                " 	"+
                "	vec2 coord;"+
                //top
                "	if ((texCoord.y <= 1.0) &&(texCoord.y >= 2.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){"+
                "		coord.y = (texCoord.y - 2.0/3.0) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3(coord.x, -1.0, -coord.y));"+
                "	}else"+
                "	if ((texCoord.y >= 0.0) &&(texCoord.y <= 1.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){"+
                "		coord.y = (texCoord.y) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3(coord.x, 1.0, coord.y));"+
                "	}else"+
                //front
                "	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){"+
                "		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3( coord.x, -coord.y, +1.0));"+
                "	}else"+
                "	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 3.0/4.0) && (texCoord.x <= 4.0/4.0)){"+
                "		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x - 3.0/4.0) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3( -coord.x, -coord.y, -1.0));"+
                "	}else"+
                //left
                "	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 0.0) && (texCoord.x <= 1.0/4.0)){"+
                "		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x ) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3( -1.0, -coord.y, coord.x));"+
                "	}else"+
                "	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 1.0/2.0) && (texCoord.x <= 3.0/4.0)){"+
                "		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;"+
                "		coord.x = (texCoord.x - 2.0/4.0) * 4.0 * 2.0 - 1.0;"+
                "		gl_FragColor = textureCube(drawTexture, vec3( +1.0, -coord.y, -coord.x));"+
                "	} else"+
                "		discard;"+
                "}"
            ;
        public Viewer(int maxGLESversion) {
            super(maxGLESversion, ShaderUtils.loadProgramFromSource(maxGLESversion, SHADER_VERT_SRC, SHADER_FRAG_SRC, null ));
        }

        @Override
        public void view(int textureID, double x, double y, double scale, double aspectXY, int level) {
            if (shaderProgram > 0) {
                GLES20.glUseProgram(shaderProgram);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glEnable(GLES20.GL_TEXTURE_CUBE_MAP);
                GLES20.glUniformMatrix4fv(locMat, 1, false, ToFloatArray
                        .convert(new Mat4Scale(scale * aspectXY, scale, 1).mul(new Mat4Transl(x, y, 0))), 0);
                GLES20.glUniform1i(locLevel, level);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureID);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "drawTexture"), 0);
                buffers.draw(GLES20.GL_TRIANGLE_STRIP, shaderProgram);
                GLES20.glDisable(GLES20.GL_TEXTURE_CUBE_MAP);
                GLES20.glUseProgram(0);
            }
        }
    }

    private OGLTextureCube() {
        targetSize = new TargetSize[6];
        GLES20.glGenTextures(1, textureID, 0);
        bind();
    }
    private void setTarget(Bitmap data, int target) {
        targetSize[target] = new TargetSize(data.getWidth(), data.getHeight());

        GLUtils.texImage2D(TARGETS[target], 0, GLUtils.getInternalFormat(data), data, GLUtils.getType(data), 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

    }

    public OGLTextureCube(Context context, String[] fileNames) {
        this();
        for (int i = 0; i < fileNames.length; i++) {
            Bitmap data;
            data = OGLTexture2D.readTextureDataFromFile(context, fileNames[i]);
            setTarget(data, i);
        }
    }

    public OGLTextureCube(Context context, String fileName, String[] suffixes) {
        this();
        String baseName=fileName.substring(0,fileName.lastIndexOf('.'));
        String suffix=fileName.substring(fileName.lastIndexOf('.')+1,fileName.length());
        for (int i = 0; i < suffixes.length; i++) {
            String fullName = baseName + suffixes[i] + "." + suffix;
            Bitmap data;
            data = OGLTexture2D.readTextureDataFromFile(context, fullName);
            setTarget(data, i);
        }
    }

    public void bind() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureID[0]);
    }

    @Override
    public void bind(int shaderProgram, String name, int slot) {
        bind();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + slot);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, name), slot);
    }

    @Override
    public void bind(int shaderProgram, String name) {
        bind(shaderProgram, name, 0);
    }

    @Override
    public int getTextureId(){
        return textureID[0];
    }

    public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTextureBuffer(
            OGLTexImage.Format<OGLTexImageType> format, Buffer buffer, int cubeFaceIndex) {
        bind();
        GLES20.glTexSubImage2D(TARGETS[cubeFaceIndex], 0, 0, 0,
                targetSize[cubeFaceIndex].getWidth(),targetSize[cubeFaceIndex].getHeight(),
                format.getPixelFormat(), format.getPixelType(), buffer);
    }

    public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> Buffer getTextureBuffer(
            OGLTexImage.Format<OGLTexImageType> format, int cubeFaceIndex) {
        bind();
        Buffer buffer = ByteBuffer.allocateDirect(targetSize[cubeFaceIndex].getWidth()
                * targetSize[cubeFaceIndex].getHeight() * 4);
    //    GLES20.glGetTexImage(TARGETS[cubeFaceIndex], 0, format.getPixelFormat(), format.getPixelType(), buffer);
        int[] frameBufferID = new int[1];
        GLES20.glGenFramebuffers(1, frameBufferID, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferID[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureID[0],0);
        GLES20.glReadPixels(0, 0, targetSize[cubeFaceIndex].getWidth(), targetSize[cubeFaceIndex].getHeight(),format.getPixelFormat(), format.getPixelType(), buffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return buffer;
    }

    public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTexImage(OGLTexImageType image, int cubeFaceIndex) {
        setTextureBuffer(image.getFormat(), image.getDataBuffer(), cubeFaceIndex);
    }

    public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexImageType getTexImage(
            OGLTexImage.Format<OGLTexImageType> format, int cubeFaceIndex) {
        OGLTexImageType image = format.newTexImage(
                targetSize[cubeFaceIndex].getWidth(),  targetSize[cubeFaceIndex].getHeight());
        image.setDataBuffer(getTextureBuffer(format, cubeFaceIndex));
        return image;
    }

}
