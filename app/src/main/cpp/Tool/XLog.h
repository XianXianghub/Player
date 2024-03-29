//
// Created by unionman on 2020/4/1.
//

#ifndef FFMPEG_XLOG_H
#define FFMPEG_XLOG_H


#include <android/log.h>


#define FF_LOG_TAG     "XPlay"

#define XLOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"XPlay",__VA_ARGS__)
#define XLOGI(...) __android_log_print(ANDROID_LOG_INFO, "XPlay",__VA_ARGS__)
#define XLOGE(...) __android_log_print(ANDROID_LOG_ERROR, "XPlay",__VA_ARGS__)

#define ALOG(level, TAG, ...)    ((void)__android_log_print(level, TAG, __VA_ARGS__))



class XLog{


};

#endif //FFMPEG_XLOG_H
