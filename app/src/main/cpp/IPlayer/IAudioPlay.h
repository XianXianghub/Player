//
// Created by unionman on 2020/4/10.
//

#ifndef FFMPEG_IAUDIOPLAY_H
#define FFMPEG_IAUDIOPLAY_H

#include <list>
#include <mutex>

#include <IObserver.h>
#include <XParameter.h>

class IAudioPlay : public IObserver{
public:
    virtual bool StartPlay(XParameter out) = 0;
    void Update(XData data) override;
    virtual XData GetData();
    virtual void Close()=0;
    virtual void Clear();
    int  maxFrame = 500;
    int pts=0;
protected:
    std::list <XData> frames;
    std::mutex frameMute;
};


#endif //FFMPEG_IAUDIOPLAY_H
