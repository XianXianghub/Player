#include <jni.h>
#include <string>
#include <android/log.h>
#include<iostream>
#include <IDecode.h>
#include <FFDecode.h>
#include "FFmpeg/FFDemux.h"
#include "IPlayer/IDemux.h"
#include "XThread.h"
#include "XLog.h"
#include "Java_com_bds_ffmpeg_UPlayer_Start.h"
#include <android/native_window_jni.h>
#include <XEGL.h>
#include <XShader.h>
#include <IVideoView.h>
#include <GLVideoView.h>
#include <IResample.h>
#include <FFResample.h>
#include <SLAudioPlay.h>
#include <IPlayer.h>
#include <IPlayerPorxy.h>

extern "C"{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavcodec/jni.h>
}
IVideoView *view = NULL;
extern "C"
JNIEXPORT
jint JNI_OnLoad(JavaVM *vm,void *res)
{
    IPlayerPorxy::Get()->Init(vm);
    return JNI_VERSION_1_4;
}




extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_XPlay_InitView(JNIEnv *env, jobject thiz, jobject obj) {
    // TODO: implement InitView()
    ANativeWindow *win = ANativeWindow_fromSurface(env,obj);
   // view->setRender(win);
    IPlayerPorxy::Get()->InitView(win);
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_OpenUrl_Open(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    IPlayerPorxy::Get()->Open(url);
    IPlayerPorxy::Get()->Start();
    env->ReleaseStringUTFChars(url_, url);
}extern "C"
JNIEXPORT jdouble JNICALL
Java_com_bds_ffmpeg_VideoViewActivity_PlayPos(JNIEnv *env, jobject thiz) {
    return IPlayerPorxy::Get()->PlayPos();
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_VideoViewActivity_Seek(JNIEnv *env, jobject thiz, jdouble pos) {
    IPlayerPorxy::Get()->Seek(pos);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_VideoViewActivity_Open(JNIEnv *env, jobject thiz, jstring url) {
    // TODO: implement Open()
    const char *jniurl = env->GetStringUTFChars(url, 0);
    IPlayerPorxy::Get()->Open(jniurl);
    IPlayerPorxy::Get()->Start();
    env->ReleaseStringUTFChars(url, jniurl);
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_VideoActivity_PlayOrPause(JNIEnv *env, jobject thiz) {
    // TODO: implement PlayOrPause()

    IPlayerPorxy::Get()->SetPause(!IPlayerPorxy::Get()->IsPause());
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_VideoViewActivity_PlayOrPause(JNIEnv *env, jobject thiz) {
    // TODO: implement PlayOrPause()
    IPlayerPorxy::Get()->SetPause(!IPlayerPorxy::Get()->IsPause());
}




extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_InitView(JNIEnv *env, jobject thiz, jobject obj) {
    // TODO: implement InitView()
    ANativeWindow *win = ANativeWindow_fromSurface(env,obj);
    // view->setRender(win);
    IPlayerPorxy::Get()->InitView(win);
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_Open(JNIEnv *env, jobject thiz, jstring url) {
    // TODO: implement Open()
    const char *jniurl = env->GetStringUTFChars(url, 0);
    IPlayerPorxy::Get()->Open(jniurl);
    env->ReleaseStringUTFChars(url, jniurl);
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_Seek(JNIEnv *env, jobject thiz, jdouble pos) {
    // TODO: implement Seek()
    IPlayerPorxy::Get()->Seek(pos);
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_Pause(JNIEnv *env, jobject thiz) {
    // TODO: implement Pause()
    IPlayerPorxy::Get()->SetPause(true);
    XLOGE("IPlayerPorxy::Get()->SetPause(true)");
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_play(JNIEnv *env, jobject thiz) {
    // TODO: implement play()
    IPlayerPorxy::Get()->SetPause(false);
}extern "C"
JNIEXPORT jdouble JNICALL
Java_com_bds_ffmpeg_UPlayer_GetPos(JNIEnv *env, jobject thiz) {
    // TODO: implement GetPos()
    return IPlayerPorxy::Get()->PlayPos();
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_Start(JNIEnv *env, jobject thiz) {
    // TODO: implement Start()
    IPlayerPorxy::Get()->Start();
}extern "C"
JNIEXPORT jint JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1getDuration(JNIEnv *env, jobject thiz) {
    // TODO: implement native_getDuration()
    return IPlayerPorxy::Get()->getDuration();
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1isPlaying(JNIEnv *env, jobject thiz) {
    // TODO: implement native_isPlaying()
    XLOGE("IPlayerPorxy::Get()->IsPause()=%d",IPlayerPorxy::Get()->IsPause());
    return !IPlayerPorxy::Get()->IsPause();
}extern "C"
JNIEXPORT jint JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1getPlayerStatus(JNIEnv *env, jobject thiz) {
    // TODO: implement native_getPlayerStatus()
    return IPlayerPorxy::Get()->getPlayStatus();
}extern "C"
JNIEXPORT jint JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1getVideoHeight(JNIEnv *env, jobject thiz) {
    // TODO: implement native_getVideoHeight()
    return IPlayerPorxy::Get()->getVideoHeight();
}extern "C"
JNIEXPORT jint JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1getVideoWidth(JNIEnv *env, jobject thiz) {
    // TODO: implement native_getVideoWidth()
    return IPlayerPorxy::Get()->getVideoWidth();
}extern "C"
JNIEXPORT void JNICALL
Java_com_bds_ffmpeg_UPlayer_native_1release(JNIEnv *env, jobject thiz) {
    // TODO: implement native_release()
    IPlayerPorxy::Get()->Close();
}