//
// Created by unionman on 2020/4/1.
//
extern "C"{
#include <libavformat/avformat.h>
#include <libavutil/log.h>
}

#include <XData.h>
#include <android/log.h>
#include "FFDemux.h"
#include "XLog.h"

static void ffp_log_callback_report(void *ptr, int level, const char *fmt, va_list vl)
{
    int ffplv = ANDROID_LOG_VERBOSE;



    va_list vl2;
    char line[1024];
    static int print_prefix = 1;


    va_copy(vl2, vl);
    // av_log_default_callback(ptr, level, fmt, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, sizeof(line), &print_prefix);
    va_end(vl2);

    if (level <= AV_LOG_ERROR)
        ffplv = ANDROID_LOG_ERROR;
    else if (level <= AV_LOG_WARNING)
        ffplv = ANDROID_LOG_WARN;
    else if (level <= AV_LOG_INFO)
        ffplv = ANDROID_LOG_INFO;
    else if (level <= AV_LOG_VERBOSE)
        ffplv = ANDROID_LOG_VERBOSE;
    else
        ffplv = ANDROID_LOG_DEBUG;

    ALOG(ffplv, FF_LOG_TAG, "%s", line);
}
static void log_callback_null(void *ptr, int level, const char *fmt, va_list vl)
{
}
bool FFDemux::Open(const  char *url) {
    Close();
    mux.lock();
    XLOGI("open url %s",url);
    int ret = avformat_open_input(&ic,url,0,0);
    if(ret !=0){
        mux.unlock();
        XLOGE("open failed %s",url);
        return false;
    }

    av_dump_format(ic, 0, url, 0);

   // av_log_set_callback(log_callback_null);
    XLOGE("avformat_open_input success");
    ret = avformat_find_stream_info(ic,0);
    if(ret !=0){
        mux.unlock();
        XLOGE("avformat_find_stream_info failed");
        return false;
    }
    XLOGE("avformat_find_stream_info success");

    this->total = ic->duration/(AV_TIME_BASE/1000);

    mux.unlock();
    XLOGI("this->total= %d",total);
    GetVPara();
    GetAPara();
    return true;
}
static double r2d(AVRational r)
{
    return r.num == 0 || r.den == 0 ?0.:(double) r.num/(double)r.den;
}
static int cnt =0;
XData FFDemux::Read() {
    mux.lock();
    if(!ic){
        mux.unlock();
        return XData();
    }
    XData data;
    AVPacket *pkt = av_packet_alloc();
    int re = av_read_frame(ic,pkt);
    if(re!=0){
    //    XLOGE("av_read_frame read failed %d",re);
        mux.unlock();
        av_packet_free(&pkt);
        return XData();
    }
    data.data = (unsigned char *)pkt;
    data.size = pkt->size;
    data.pts = pkt->pts;
    if(pkt->stream_index == audioStream)
    {
        data.isAudio = true;
    }
    else if(pkt->stream_index == videoStream)
    {
        data.isAudio = false;
    }
    else
    {
        mux.unlock();
        av_packet_free(&pkt);
        return XData();
    }

        //转换pts
        pkt->pts = pkt->pts * (1000*r2d(ic->streams[pkt->stream_index]->time_base));
        pkt->dts = pkt->dts * (1000*r2d(ic->streams[pkt->stream_index]->time_base));
        data.pts = (int)pkt->pts;
    mux.unlock();
    return data;
}

FFDemux::FFDemux() {
    static bool isFirst = true;
    if(isFirst){
        isFirst = false;
        XLOGE("av_register_all ");
    //    av_log_set_callback(ffp_log_callback_report);
    //注册封装器
        av_register_all();

        avformat_network_init();
        //注册解码器
        avcodec_register_all();
        //初始化网络

    }
}

XParameter FFDemux::GetVPara() {
    mux.lock();
    if (!ic) {
        mux.unlock();
        XLOGE("GetVPara failed! ic is NULL！");
        return XParameter();
    }
    //获取了视频流索引
    int re = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (re < 0) {
        mux.unlock();
        XLOGE("av_find_best_stream failed!");
        return XParameter();
    }
    videoStream = re;
    XParameter para;
//    if(ic->streams[videoStream]->duration>0){
//        this->total =ic->streams[videoStream]->duration/(AV_TIME_BASE/1000);
//    }
    VideoWidth = ic->streams[re]->codec->width;
    VideoHeight = ic->streams[re]->codec->height;
    para.para = ic->streams[re]->codecpar;

    mux.unlock();
    return para;
}

XParameter FFDemux::GetAPara() {
    mux.lock();
    if (!ic) {
        mux.unlock();
        XLOGE("GetVPara failed! ic is NULL！");
        return XParameter();
    }
    //获取了音频流索引
    int re = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (re < 0) {
        mux.unlock();
        XLOGE("av_find_best_stream failed!");
        return XParameter();
    }
    audioStream = re;
    XParameter para;\
    para.channels= ic->streams[re]->codecpar->channels;
    para.sample_rate = ic->streams[re]->codecpar->sample_rate;
    para.para = ic->streams[re]->codecpar;
    mux.unlock();
    return para;
}

void FFDemux::Close() {
    mux.lock();
    if(ic)
        avformat_close_input(&ic);
    mux.unlock();
}

bool FFDemux::Seek(double pos) {
    if(pos<0 || pos > 1)
    {
        XLOGE("Seek value must 0.0~1.0");
        return false;
    }
    bool re = false;
    mux.lock();
    if(!ic)
    {
        mux.unlock();
        return false;
    }
    //清理读取的缓冲
    avformat_flush(ic);
    long long seekPts = 0;
    seekPts = total*pos/r2d(ic->streams[videoStream]->time_base)/1000;
    XLOGE("demux seekpts==%lld",seekPts);
    //往后跳转到关键帧
    re = av_seek_frame(ic,videoStream,seekPts,AVSEEK_FLAG_FRAME|AVSEEK_FLAG_BACKWARD);
    mux.unlock();
    return re;
}


