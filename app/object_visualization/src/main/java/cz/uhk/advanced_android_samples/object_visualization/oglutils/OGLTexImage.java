package cz.uhk.advanced_android_samples.object_visualization.oglutils;

import java.nio.Buffer;

public interface OGLTexImage<OGLTexImageType> {
    static interface Format<OGLTexImageType> {
        int getInternalFormat();
        int getPixelFormat();
        int getPixelType();
        int getComponentCount();
        Buffer newBuffer(int width, int height);
        Buffer newBuffer(int width, int height, int depth);
        OGLTexImageType newTexImage(int width, int height);
        OGLTexImageType newTexImage(int width, int height, int depth);
    }
    int getWidth();
    int getHeight();
    int getDepth();
    void setDataBuffer(Buffer buffer);
    Buffer getDataBuffer();
    Format<OGLTexImageType> getFormat();
}
