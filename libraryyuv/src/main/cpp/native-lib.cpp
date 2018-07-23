//
// Created by DELL on 2018/3/26.
//

#include <jni.h>
#include <string>
#include <string.h>
#include "libyuv.h"

extern "C" {

    JNIEXPORT
    JNICALL
    void Java_org_android_opensource_libraryyuv_Libyuv_ARGBToNV21(
            JNIEnv *env, jclass *jcls,
            jbyteArray src_frame, jint src_stride_frame,
            jint width, jint height,
            jbyteArray yBuffer, jbyteArray uvBuffer) {

        uint8_t* srcFrame = (uint8_t*) env->GetByteArrayElements(src_frame, 0);

        uint8_t* dst_y=(uint8_t*) env->GetByteArrayElements(yBuffer, 0);
        uint8_t* dst_uv=(uint8_t*) env->GetByteArrayElements(uvBuffer, 0);


        libyuv::ARGBToNV21(srcFrame, src_stride_frame,
                           dst_y, width,
                           dst_uv, width, width, height);

        //remember release
        env->ReleaseByteArrayElements(src_frame, (jbyte*)srcFrame, 0);
        env->ReleaseByteArrayElements(yBuffer, (jbyte*)dst_y, 0);
        env->ReleaseByteArrayElements(uvBuffer, (jbyte*)dst_uv, 0);
    }

    JNIEXPORT void
    JNICALL
    Java_org_android_opensource_libraryyuv_Libyuv_ARGBMirror(
                JNIEnv *env, jclass *jcls,
                jbyteArray srcBuffer, jint srcStride,
                jbyteArray dstBuffer,jint dstStride,
                jint width, jint height)
    {
        uint8_t* src_argb=(uint8_t*) env->GetByteArrayElements(srcBuffer, 0);
        uint8_t* dst_argb=(uint8_t*) env->GetByteArrayElements(dstBuffer, 0);

        libyuv::ARGBMirror(src_argb, srcStride, dst_argb, dstStride,
                            width, height);

        env->ReleaseByteArrayElements(srcBuffer, (jbyte*)src_argb, 0);
        env->ReleaseByteArrayElements(dstBuffer, (jbyte*)dst_argb, 0);
    }

//int NV21ToARGB(
//               const uint8* src_y, int src_stride_y,
//               const uint8* src_vu, int src_stride_vu,
//               uint8* dst_argb, int dst_stride_argb,
//               int width, int height);

    JNIEXPORT void
    JNICALL
    Java_org_android_opensource_libraryyuv_Libyuv_NV21ToARGB(
            JNIEnv *env, jclass *jcls,
            jbyteArray yBuffer,jint y_stride,
            jbyteArray uvBuffer,jint uv_stride,
            jbyteArray dstARGB,jint dst_stride_argb,
            jint width, jint height) {

        uint8_t* src_y=(uint8_t*) env->GetByteArrayElements(yBuffer, 0);
        uint8_t* src_uv=(uint8_t*) env->GetByteArrayElements(uvBuffer, 0);
        uint8_t* dst_argb = (uint8_t*) env->GetByteArrayElements(dstARGB, 0);

        libyuv::NV21ToARGB(src_y, y_stride, src_uv, uv_stride, dst_argb, dst_stride_argb, width, height);

        //remember release
        env->ReleaseByteArrayElements(dstARGB, (jbyte*)dst_argb, 0);
        env->ReleaseByteArrayElements(yBuffer, (jbyte*)src_y, 0);
        env->ReleaseByteArrayElements(uvBuffer, (jbyte*)src_uv, 0);

    }

JNIEXPORT
JNICALL
void Java_org_android_opensource_libraryyuv_Libyuv_RGBAToARGB(
        JNIEnv *env, jclass *jcls,
        jbyteArray srcBuffer,jint src_stride_frame,
        jbyteArray dstBuffer, jint dst_stride_argb,
        jint width, jint height) {

    uint8_t* src_frame = (uint8_t*) env->GetByteArrayElements(srcBuffer, 0);
    uint8_t* dst_argb = (uint8_t*) env->GetByteArrayElements(dstBuffer, 0);

    libyuv::RGBAToARGB(src_frame,src_stride_frame, dst_argb,dst_stride_argb, width, height);

    //remember release
    env->ReleaseByteArrayElements(srcBuffer, (jbyte*)src_frame, 0);
    env->ReleaseByteArrayElements(dstBuffer, (jbyte*)dst_argb, 0);
}

JNIEXPORT
JNICALL
void Java_org_android_opensource_libraryyuv_Libyuv_ARCORETONV21(
        JNIEnv *env, jclass *jcls,
        jbyteArray srcBuffer,jint src_stride_frame,
        jbyteArray tempBuffer,
        jbyteArray dstBuffer, jint dst_stride_frame,
        jint width, jint height, jbyteArray yBuffer, jbyteArray uvBuffer, jbyteArray frameData) {

    uint8_t* src_frame = (uint8_t*) env->GetByteArrayElements(srcBuffer, 0);
    uint8_t* dst_frame = (uint8_t*) env->GetByteArrayElements(dstBuffer, 0);
    uint8_t* temp_frame = (uint8_t*) env->GetByteArrayElements(tempBuffer, 0);
    uint8_t* frame_data = (uint8_t*) env->GetByteArrayElements(frameData, 0);

    libyuv::ABGRToARGB(src_frame, src_stride_frame, temp_frame, dst_stride_frame, width, height);
//    libyuv::ARGBMirror(temp_frame, dst_stride_frame, dst_frame, dst_stride_frame, width, -height);

    uint8_t* dst_y=(uint8_t*) env->GetByteArrayElements(yBuffer, 0);
    uint8_t* dst_uv=(uint8_t*) env->GetByteArrayElements(uvBuffer, 0);

    libyuv::ARGBToNV21(temp_frame, dst_stride_frame,
                       dst_y, width,
                       dst_uv, width, width, height);

    memcpy(frame_data, dst_y, width * -height);

    uint8_t* uvOffset = &frame_data[width * -height];

    memcpy(uvOffset, dst_uv, width * -height / 2);

    //remember release
    env->ReleaseByteArrayElements(srcBuffer, (jbyte*)src_frame, 0);
    env->ReleaseByteArrayElements(dstBuffer, (jbyte*)dst_frame, 0);
    env->ReleaseByteArrayElements(frameData, (jbyte*)frame_data, 0);
    env->ReleaseByteArrayElements(yBuffer, (jbyte*)dst_y, 0);
    env->ReleaseByteArrayElements(uvBuffer, (jbyte*)dst_uv, 0);
}


JNIEXPORT
JNICALL
void Java_org_android_opensource_libraryyuv_Libyuv_ABGRToARGB(
        JNIEnv *env, jclass *jcls,
        jbyteArray srcBuffer,jint src_stride_frame,
        jbyteArray dstBuffer, jint dst_stride_argb,
        jint width, jint height) {

    uint8_t* src_frame = (uint8_t*) env->GetByteArrayElements(srcBuffer, 0);
    uint8_t* dst_argb = (uint8_t*) env->GetByteArrayElements(dstBuffer, 0);

    libyuv::ABGRToARGB(src_frame,src_stride_frame, dst_argb,dst_stride_argb, width, height);

    //remember release
    env->ReleaseByteArrayElements(srcBuffer, (jbyte*)src_frame, 0);
    env->ReleaseByteArrayElements(dstBuffer, (jbyte*)dst_argb, 0);
}

JNIEXPORT
JNICALL
void Java_org_android_opensource_libraryyuv_Libyuv_ARGBScale(
        JNIEnv *env, jclass *jcls,
        jbyteArray srcBuffer, jint src_stride_frame,
        jint srcwidth, jint srcheight,
        jbyteArray dstBuffer, jint dst_stride_frame,
        jint dstwidth, jint dstheight) {

    uint8_t* src_frame = (uint8_t*) env->GetByteArrayElements(srcBuffer, 0);
    uint8_t* dst_frame = (uint8_t*) env->GetByteArrayElements(dstBuffer, 0);

    libyuv::ARGBScale(src_frame, src_stride_frame, srcwidth, srcheight, dst_frame, dst_stride_frame, dstwidth, dstheight, libyuv::kFilterNone);

    //remember release
    env->ReleaseByteArrayElements(srcBuffer, (jbyte*)src_frame, 0);
    env->ReleaseByteArrayElements(dstBuffer, (jbyte*)dst_frame, 0);
}


}