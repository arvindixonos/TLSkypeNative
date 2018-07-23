package org.android.opensource.libraryyuv;

public class Libyuv {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("yuv");
    }

    public native static void ARGBToNV21(byte[] src_frame,int src_stride,
                                  int width, int height, byte[] yBuffer, byte[] uvBuffer);


    public native static void NV21ToARGB(byte[] yBuffer,int y_stride,
                                         byte[] uvBuffer,int uv_stride,
                                         byte[] dstARGB,int dst_stride_argb,
                                         int width, int height);

    public native static void RGBAToARGB(byte[] srcBuffer,int src_stride_frame,
                                         byte[] dstBuffer, int dst_stride_argb,
                                         int width, int height);

    public native static void ABGRToARGB(byte[] srcBuffer,int src_stride_frame,
                                         byte[] dstBuffer, int dst_stride_argb,
                                         int width, int height);


    public native static void ARCORETONV21(byte[] srcBuffer,int src_stride_frame, byte[] tempBuffer,
                                         byte[] dstBuffer, int dst_stride_argb,
                                         int width, int height, byte[] yBuffer, byte[] uvBuffer, byte[] frameData);

    public native static void ARGBMirror(byte[] srcBuffer,int src_stride_frame,
                                         byte[] dstBuffer, int dst_stride_argb,
                                         int width, int height);


    public native static void ARGBScale(byte[] srcBuffer,int src_stride_frame,
                                        int srcWidth, int srcHeight,
                                         byte[] dstBuffer, int dst_stride_frame,
                                        int dstWidth, int dstHeight);
}
