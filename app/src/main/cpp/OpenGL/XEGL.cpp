//
// Created by bds on 2020/4/8.
//

#include "XEGL.h"
#include <android/native_window.h>
#include <EGL/egl.h>
#include <XLog.h>
#include <mutex>

class CXEGL : public XEGL{
public:
    void Close() override {
        mux.lock();
        if(display == EGL_NO_DISPLAY)
        {
            mux.unlock();
            return;
        }
        eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);

        if(surface != EGL_NO_SURFACE)
            eglDestroySurface(display,surface);
        if(context != EGL_NO_CONTEXT)
            eglDestroyContext(display,context);

        eglTerminate(display);

        display = EGL_NO_DISPLAY;
        surface = EGL_NO_SURFACE;
        context = EGL_NO_CONTEXT;
        mux.unlock();
    }

private:
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    std::mutex mux;
public:
    void Draw() override {
        mux.lock();
        if(display == EGL_NO_DISPLAY || surface == EGL_NO_SURFACE)
        {
            mux.unlock();
            return;
        }
        eglSwapBuffers(display,surface);
        mux.unlock();
    }

private:
    virtual bool Init(void *win) {
        XLOGE("egl start init ");
        ANativeWindow *nwin = (ANativeWindow *)win;
        Close();
        mux.lock();
        //1 获取EGLDisplay对象 显示设备
        display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if(display == EGL_NO_DISPLAY)
        {
            mux.unlock();
            XLOGE("eglGetDisplay failed!");
            return false;
        }
        XLOGE("eglGetDisplay success!");
        //2 初始化Display
        if(EGL_TRUE != eglInitialize(display,0,0)){
            mux.unlock();
            XLOGE("eglInitialize failed!");
            return false;
        }
        XLOGE("eglInitialize success!");
        //3 获取配置并创建surface
        //3 获取配置并创建surface
        EGLint configSpec [] = {
                EGL_RED_SIZE,8,
                EGL_GREEN_SIZE,8,
                EGL_BLUE_SIZE,8,
                EGL_SURFACE_TYPE,EGL_WINDOW_BIT,
                EGL_NONE
        };
        EGLConfig config = 0;
        EGLint numConfigs = 0;
        if(EGL_TRUE != eglChooseConfig(display,configSpec,&config,1,&numConfigs))
        {
            mux.unlock();
            XLOGE("eglChooseConfig failed!");
            return false;
        }
        XLOGE("eglChooseConfig success!");
        surface = eglCreateWindowSurface(display,config,nwin,NULL);

        //4 创建并打开EGL上下文
        const EGLint ctxAttr[] = { EGL_CONTEXT_CLIENT_VERSION ,2, EGL_NONE};
        context = eglCreateContext(display,config,EGL_NO_CONTEXT,ctxAttr);
        if(context == EGL_NO_CONTEXT)
        {
            mux.unlock();
            XLOGE("eglCreateContext failed!");
            return false;
        }
        XLOGE("eglCreateContext success!");

        if(EGL_TRUE != eglMakeCurrent(display,surface,surface,context))
        {
            mux.unlock();
            XLOGE("eglMakeCurrent failed!");
            return false;
        }
        mux.unlock();
        XLOGE("eglMakeCurrent success!");
        return true;
    }
};

XEGL *XEGL::Get() {
    static CXEGL egl;
    return &egl;
}
