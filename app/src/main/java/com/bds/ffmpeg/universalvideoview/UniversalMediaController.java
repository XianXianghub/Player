/*
* Copyright (C) 2015 Author <dictfb#gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.bds.ffmpeg.universalvideoview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bds.ffmpeg.R;

import java.util.Formatter;
import java.util.Locale;

public class UniversalMediaController extends FrameLayout {


    private MediaPlayerControl mPlayer;

    private Context mContext;

    private ProgressBar mProgress;

    private TextView mEndTime, mCurrentTime;

    private TextView mTitle;

    private boolean mShowing = true;

    private boolean mDragging;


    private boolean mScalable = false;
    private boolean mIsFullScreen = false;
//    private boolean mFullscreenEnabled = false;


    private static final int sDefaultTimeout = 3000;

    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_LOADING = 3;
    private static final int STATE_ERROR = 4;
    private static final int STATE_COMPLETE = 5;

    private int mState = STATE_LOADING;


    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SHOW_LOADING = 3;
    private static final int HIDE_LOADING = 4;
    private static final int SHOW_ERROR = 5;
    private static final int HIDE_ERROR = 6;
    private static final int SHOW_COMPLETE = 7;
    private static final int HIDE_COMPLETE = 8;
    StringBuilder mFormatBuilder;
    protected GestureDetector mGestureDetector;
    Formatter mFormatter;

    private ImageButton mTurnButton;// 开启暂停按钮

    private ImageButton mScaleButton;

    private View mBackButton;// 返回按钮

    private ViewGroup loadingLayout;

    private ViewGroup errorLayout;

    private View mTitleLayout;
    private View mControlLayout;

    private View mCenterPlayButton;

    public UniversalMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.UniversalMediaController);
        mScalable = a.getBoolean(R.styleable.UniversalMediaController_uvv_scalable, false);
        a.recycle();
        init(context);
    }

    public UniversalMediaController(Context context) {
        super(context);
        init(context);
    }
    private VideoGestureListener videoGestureListener;
    private void init(Context context) {
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewRoot = inflater.inflate(R.layout.uvv_player_controller, this);
        videoGestureListener = new VideoGestureListener(this);
        mGestureDetector = new GestureDetector(getContext(), videoGestureListener);
        setOnTouchListener(mTouchListener);
        initControllerView(viewRoot);
    }


    private void initControllerView(View v) {
        mTitleLayout = v.findViewById(R.id.title_part);
        mControlLayout = v.findViewById(R.id.control_layout);
        loadingLayout = (ViewGroup) v.findViewById(R.id.loading_layout);
        errorLayout = (ViewGroup) v.findViewById(R.id.error_layout);
        mTurnButton = (ImageButton) v.findViewById(R.id.turn_button);
        mScaleButton = (ImageButton) v.findViewById(R.id.scale_button);
        mCenterPlayButton = v.findViewById(R.id.center_play_btn);
        mBackButton = v.findViewById(R.id.back_btn);


//        setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    if (mShowing) {
//                        hide();
//                        handled = true;
//                    }
//                }
//                if (mGestureDetector.onTouchEvent(event)) return true;
//                return videoGestureListener.onTouch(v, event);
//            }
//        });
        if (mTurnButton != null) {
            mTurnButton.requestFocus();
            mTurnButton.setOnClickListener(mPauseListener);
        }

        if (mScalable) {
            if (mScaleButton != null) {
                mScaleButton.setVisibility(VISIBLE);
                mScaleButton.setOnClickListener(mScaleListener);
            }
        } else {
            if (mScaleButton != null) {
                mScaleButton.setVisibility(GONE);
            }
        }

        if (mCenterPlayButton != null) {//重新开始播放
            mCenterPlayButton.setOnClickListener(mCenterPlayListener);
        }

        if (mBackButton != null) {//返回按钮仅在全屏状态下可见
            mBackButton.setOnClickListener(mBackListener);
        }

        View bar = v.findViewById(R.id.seekbar);
        mProgress = (ProgressBar) bar;
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(100);
        }

        mEndTime = (TextView) v.findViewById(R.id.duration);
        mCurrentTime = (TextView) v.findViewById(R.id.has_played);
        mTitle = (TextView) v.findViewById(R.id.title);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    private UniversalVideoView videoView;
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }
    void setTarget(UniversalVideoView v){
        videoView = v;
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }


    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(int timeout) {//只负责上下两条bar的显示,不负责中央loading,error,playBtn的显示.
        if (!mShowing) {
            setProgress();
            if (mTurnButton != null) {
                mTurnButton.requestFocus();
            }

            mShowing = true;
        }
        updatePausePlay();
        updateBackButton();

        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        if (mTitleLayout.getVisibility() != VISIBLE) {
            mTitleLayout.setVisibility(VISIBLE);
        }
        if (mControlLayout.getVisibility() != VISIBLE) {
            mControlLayout.setVisibility(VISIBLE);
        }

        // cause the progress bar to be updated even if mShowing
        // was already true. This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }
    public void hideView(){
        Message msg = mHandler.obtainMessage(FADE_OUT);
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(msg, 2000);

    }
    public boolean isShowing() {
        return mShowing;
    }


    public void hide() {//只负责上下两条bar的隐藏,不负责中央loading,error,playBtn的隐藏
        if (mShowing) {
            mHandler.removeMessages(SHOW_PROGRESS);
            mTitleLayout.setVisibility(GONE);
         //   Log.d("XPLAY", " mControlLayout.setVisibility(GONE)");
            mControlLayout.setVisibility(GONE);
            mShowing = false;
            hideCenterView();
      }
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
//            Log.d("XPLAY", "msg.what=="+msg.what);
            switch (msg.what) {
                case FADE_OUT: //1
                    hide();
                    break;
                case SHOW_PROGRESS: //2
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case SHOW_LOADING: //3
                    show();
                    showCenterView(R.id.loading_layout);
                    break;
                case SHOW_COMPLETE: //7
                    showCenterView(R.id.center_play_btn);
                    break;
                case SHOW_ERROR: //5
                    show();
                    showCenterView(R.id.error_layout);
                    break;
                case HIDE_LOADING: //4
                case HIDE_ERROR: //6
                case HIDE_COMPLETE: //8
                    hide();
                    hideCenterView();
                    break;
            }
        }
    };

    private void showCenterView(int resId) {
        if (resId == R.id.loading_layout) {
            if (loadingLayout.getVisibility() != VISIBLE) {
                loadingLayout.setVisibility(VISIBLE);
            }
            if (mCenterPlayButton.getVisibility() == VISIBLE) {
                mCenterPlayButton.setVisibility(GONE);
            }
            if (errorLayout.getVisibility() == VISIBLE) {
                errorLayout.setVisibility(GONE);
            }
        } else if (resId == R.id.center_play_btn) {
            if (mCenterPlayButton.getVisibility() != VISIBLE) {
                mCenterPlayButton.setVisibility(VISIBLE);
            }
            if (loadingLayout.getVisibility() == VISIBLE) {
                loadingLayout.setVisibility(GONE);
            }
            if (errorLayout.getVisibility() == VISIBLE) {
                errorLayout.setVisibility(GONE);
            }

        } else if (resId == R.id.error_layout) {
            if (errorLayout.getVisibility() != VISIBLE) {
                errorLayout.setVisibility(VISIBLE);
            }
            if (mCenterPlayButton.getVisibility() == VISIBLE) {
                mCenterPlayButton.setVisibility(GONE);
            }
            if (loadingLayout.getVisibility() == VISIBLE) {
                loadingLayout.setVisibility(GONE);
            }

        }
    }


    private void hideCenterView() {
  //      Log.d("XPLAY", "msg.hideCenterView==");
        if (mCenterPlayButton.getVisibility() == VISIBLE) {
            mCenterPlayButton.setVisibility(GONE);
        }
        if (errorLayout.getVisibility() == VISIBLE) {
            errorLayout.setVisibility(GONE);
        }
        if (loadingLayout.getVisibility() == VISIBLE) {
            loadingLayout.setVisibility(GONE);
        }
    }

    public void reset() {
        mCurrentTime.setText("00:00");
        mEndTime.setText("00:00");
        mProgress.setProgress(0);
        mTurnButton.setImageResource(R.drawable.uvv_player_player_btn);
        setVisibility(View.VISIBLE);
        hideLoading();
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging || !mPlayer.isPlaying()) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 100L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));
        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
               // if (!isShowing()) {
               //     show(0); // show until hide is called
              //  }
            //    handled = false;
                break;
            case MotionEvent.ACTION_UP:
                if (!handled) {
                    handled = false;
                    show(sDefaultTimeout); // start timeout
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }
    public UniversalVideoView getTarget(){
        return videoView;
    }

    boolean handled = false;
    //如果正在显示,则使之消失
    private OnTouchListener mTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mShowing) {
                    hide();
                    handled = true;
                }else {
                    show(0); // show until hide is called
                    handled = false;
                }
            }
            boolean ret=  mGestureDetector.onTouchEvent(event);
           return videoGestureListener.onTouch(v, event);

        }
    };

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mTurnButton != null) {
                    mTurnButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            if (mPlayer != null) {
                doPauseResume();
                show(sDefaultTimeout);
            }
        }
    };

    private OnClickListener mScaleListener = new OnClickListener() {
        public void onClick(View v) {
            Log.d("Uplayer","mIsFullScreen=="+mIsFullScreen);
            mIsFullScreen = !mIsFullScreen;
            updateScaleButton();
            updateBackButton();
            mPlayer.setFullscreen(mIsFullScreen);
        }
    };
    private Activity mActivity;
    public void setfullScreen(){
        mIsFullScreen = true;
        updateScaleButton();
        updateBackButton();
        Log.d("Uplayer","mPlayer=="+mPlayer);
        mPlayer.setFullscreen(mIsFullScreen);
    }
    public void setActivity(Activity a){
        mActivity =a;
    }

    //仅全屏时才有返回按钮
    private OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
//            if (mIsFullScreen) {
//                Log.d("Uplayer","OnClickListener==");
//                mIsFullScreen = false;
//                updateScaleButton();
//                updateBackButton();
//                mPlayer.setFullscreen(false);
//            }
            mPlayer.release();
            mActivity.finish();


        }
    };

    private OnClickListener mCenterPlayListener = new OnClickListener() {
        public void onClick(View v) {
            hideCenterView();
            mPlayer.start();
        }
    };

    private void updatePausePlay() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mTurnButton.setImageResource(R.drawable.uvv_stop_btn);
            mCenterPlayButton.setVisibility(GONE);
        } else {
            mTurnButton.setImageResource(R.drawable.uvv_player_player_btn);
            mCenterPlayButton.setVisibility(VISIBLE);
        }
    }

    void updateScaleButton() {
        if (mIsFullScreen) {
            mScaleButton.setImageResource(R.drawable.uvv_star_zoom_in);
        } else {
            mScaleButton.setImageResource(R.drawable.uvv_player_scale_btn);
        }
    }

    void toggleButtons(boolean isFullScreen) {
        Log.d("Uplayer","toggleButtons=="+isFullScreen);
        mIsFullScreen = isFullScreen;
        updateScaleButton();
        updateBackButton();
    }

    void updateBackButton() {
        mBackButton.setVisibility(mIsFullScreen ? View.VISIBLE : View.INVISIBLE);
    }

    boolean isFullScreen() {
        return mIsFullScreen;
    }

    private void doPauseResume() {
        Log.d("XPLAY","    private void doPauseResume()==="+mPlayer.isPlaying());
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }
    public void onStopTrackingTouch(SeekBar bar) {
        if(mSeekListener!=null)mSeekListener.onStopTrackingTouch(bar);
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {


        boolean change = false;


        public void onStartTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }
            show(3600000);

            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null)  {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
      //      Log.d("XPLAY","seekBar.setProgress(preDuration)fromuser=="+fromuser);
            change = true;
        }

        public void onStopTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }
            if (change) {
                long duration = mPlayer.getDuration();
                 double newposition = (duration * bar.getProgress()) / 100L;
                int newval = (int) newposition;
             //   Log.d("XPLAY","11111111111seekTonewPosition="+newval);
                mPlayer.seekTo(newval);
                if (mCurrentTime != null) {
                    mCurrentTime.setText(stringForTime(newval));
                }
            }
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mShowing = true;
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
        if (mTurnButton != null) {
            mTurnButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        if (mScalable) {
            mScaleButton.setEnabled(enabled);
        }
        mBackButton.setEnabled(true);// 全屏状态下右上角的返回键总是可用.
    }

    public void showLoading() {
        mHandler.sendEmptyMessage(SHOW_LOADING);
    }

    public void hideLoading() {
        mHandler.sendEmptyMessage(HIDE_LOADING);
    }

    public void showError() {
        mHandler.sendEmptyMessage(SHOW_ERROR);
    }

    public void hideError() {
        mHandler.sendEmptyMessage(HIDE_ERROR);
    }

    public void showComplete() {
        mHandler.sendEmptyMessage(SHOW_COMPLETE);
    }

    public void hideComplete() {
        mHandler.sendEmptyMessage(HIDE_COMPLETE);
    }

    public void setTitle(String titile) {
        mTitle.setText(titile);
    }



    public void setOnErrorView(int resId) {
        errorLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(resId, errorLayout, true);
    }

    public void setOnErrorView(View onErrorView) {
        errorLayout.removeAllViews();
        errorLayout.addView(onErrorView);
    }

    public void setOnLoadingView(int resId) {
        loadingLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(resId, loadingLayout, true);
    }

    public void setOnLoadingView(View onLoadingView) {
        loadingLayout.removeAllViews();
        loadingLayout.addView(onLoadingView);
    }

    public void setOnErrorViewClick(OnClickListener onClickListener) {
        errorLayout.setOnClickListener(onClickListener);
    }

    public interface MediaPlayerControl {
        void start();

        void pause();
        public void release() ;

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        void closePlayer();//关闭播放视频,使播放器处于idle状态

        void setFullscreen(boolean fullscreen);

        /***
         *
         * @param fullscreen
         * @param screenOrientation valid only fullscreen=true.values should be one of
         *                          ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
         *                          ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
         *                          ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
         *                          ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
         */
        void setFullscreen(boolean fullscreen, int screenOrientation);
    }
}
