package com.coolcsf.testcapturedemo;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@TargetApi(23)
public class AVCEncoder {

    private final static String TAG = "AVCEncoder";
    private final static int CONFIGURE_FLAG_ENCODE = MediaCodec.CONFIGURE_FLAG_ENCODE;

    // 音视频编解码器组件
    private MediaCodec mMediaCodec;
    // 媒体数据格式信息
    private MediaFormat mMediaFormat;
    // 待编码视图宽
    private int mViewWidth;
    // 待编码视图高
    private int mViewHeight;

    private ByteBuffer configData = ByteBuffer.allocateDirect(1);
    private final static int BIT_RATE = 600000;
    private final static int FRAME_RATE = 15;
    private final static int I_FRAME_INTERVAL = 5;
    private int bitRate = BIT_RATE;
    private int frameRate = FRAME_RATE;
    private int iFrameInterval = I_FRAME_INTERVAL;

    /**
     * 视频数据信息结构体
     * 包含时间戳，视频数据，关键帧标记
     */
    static class TransferInfo {
        public long time;
        public byte[] inOutData;
        public boolean isKeyFrame;
    }

    // 待编码视频数据队列
    private final static ConcurrentLinkedQueue<TransferInfo> mInputDatasQueue = new ConcurrentLinkedQueue<TransferInfo>();
    // 已编码视频数据队列
    private final static ConcurrentLinkedQueue<TransferInfo> mOutputDatasQueue = new ConcurrentLinkedQueue<TransferInfo>();

    // 编码器回调
    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec mediaCodec, int inputBufferId) {
            try {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();
                // 从待编码视频数据队列取数据
                TransferInfo transferInfo = mInputDatasQueue.poll();
                if (transferInfo != null) {
                    inputBuffer.put(transferInfo.inOutData, 0, transferInfo.inOutData.length);
                    mediaCodec.queueInputBuffer(inputBufferId, 0, transferInfo.inOutData.length, transferInfo.time * 1000, 0);
                } else {
                    long now = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        now = SystemClock.elapsedRealtimeNanos();
                    } else {
                        now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                    }
                    // 入空数据进MediaCodec队列
                    mediaCodec.queueInputBuffer(inputBufferId, 0, 0, now * 1000, 0);
                }
            } catch (IllegalStateException exception) {
                Log.e(TAG, "encoder mediaCodec input exception: " + exception.getMessage());
            }
        }

        /**
         * 编码完成回调
         *
         */
        @Override
        public void onOutputBufferAvailable(MediaCodec mediaCodec, int outputBufferId, MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId);
            ByteBuffer keyFrameBuffer;
            if (outputBuffer != null && bufferInfo.size > 0) {
                TransferInfo transferInfo = new TransferInfo();
                transferInfo.time = bufferInfo.presentationTimeUs / 1000;
                boolean isConfigFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                if (isConfigFrame) {
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    if (configData.capacity() < bufferInfo.size) {
                        configData = ByteBuffer.allocateDirect(bufferInfo.size);
                    }
                    // 保存Codec-specific Data
                    configData.put(outputBuffer);
                }

                // 判断是否为关键帧
                boolean isKeyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                if (isKeyFrame) {
                    keyFrameBuffer = ByteBuffer.allocateDirect(
                            configData.capacity() + bufferInfo.size);
                    configData.rewind();
                    // 为关键帧时需要拼接Codec-specific Data和关键帧数据
                    keyFrameBuffer.put(configData);
                    keyFrameBuffer.put(outputBuffer);
                    keyFrameBuffer.position(0);

                    byte[] buffer = new byte[keyFrameBuffer.remaining()];
                    keyFrameBuffer.get(buffer);

                    transferInfo.inOutData = buffer;
                    transferInfo.isKeyFrame = true;

                } else {
                    byte[] buffer = new byte[outputBuffer.remaining()];
                    outputBuffer.get(buffer);

                    transferInfo.inOutData = buffer;
                    transferInfo.isKeyFrame = false;
                }
                boolean result = mOutputDatasQueue.offer(transferInfo);
            }
            mMediaCodec.releaseOutputBuffer(outputBufferId, false);
        }

        @Override
        public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
            Log.e(TAG, "encoder onError");
        }

        @Override
        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
            Log.d(TAG, "encoder onOutputFormatChanged, mediaFormat: " + mediaFormat);
        }
    };

    //
    private static MediaCodecInfo selectCodec(String mimeType) {

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    Log.d(TAG, "selectCodec OK, get " + mimeType);
                    return codecInfo;
                }
            }
        }
        return null;
    }

    // 判断设备是否支持 I420 color formats
    public static boolean isSupportI420() {
        boolean isSupport = false;
        int colorFormat = 0;
        MediaCodecInfo codecInfo = selectCodec("video/avc");
        if (codecInfo != null) {
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
            for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++) {
                int format = capabilities.colorFormats[i];
                //support color formats
                if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {    /*I420 --- YUV4:2:0 --- Nvidia Tegra 3, Samsu */
                    colorFormat = format;
                } else {
                    Log.d("Zego", " AVCEncoder unsupported color format " + format);
                }
            }
            isSupport = colorFormat != 0;
        }

        return isSupport;
    }

    public void setBitRateParam(int bitrate) {
        bitRate = bitrate;
        iFrameInterval = 15;
        setMediaFormat(bitRate, frameRate, iFrameInterval);
        mMediaCodec.stop();
        configData.clear();
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startEncoder();
    }

    /**
     * 初始化编码器
     *
     * @param viewwidth  渲染展示视图的宽
     * @param viewheight 渲染展示视图的高
     */
    public AVCEncoder(int viewwidth, int viewheight) {

        try {
            // 选用MIME类型为AVC、编码器来构造MediaCodec
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        this.mViewWidth = viewwidth;
        this.mViewHeight = viewheight;

        // 设置MediaFormat，必须设置 KEY_COLOR_FORMAT，KEY_BIT_RATE，KEY_FRAME_RATE，KEY_I_FRAME_INTERVAL的值
        setMediaFormat(bitRate, frameRate, iFrameInterval);
    }

    private void setMediaFormat(int bitRate, int frameRate, int iFrameInterval) {
        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewHeight, mViewWidth);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar); //COLOR_FormatYUV420PackedSemiPlanar
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
    }

    // 为编码器提供视频帧数据，需要 I420 格式的数据
    public void inputFrameToEncoder(byte[] needEncodeData, long timeStmp) {
        if (needEncodeData != null) {
            TransferInfo transferInfo = new TransferInfo();
            transferInfo.inOutData = needEncodeData;
            transferInfo.time = timeStmp;
            boolean inputResult = mInputDatasQueue.offer(transferInfo);
            if (!inputResult) {
                Log.d(TAG, "inputEncoder queue result = " + inputResult + " queue current size = " + mInputDatasQueue.size());
            }
        }
    }

    /**
     * 获取编码后的视频帧数据，队列为空时返回null
     */
    public TransferInfo pollFrameFromEncoder() {
        return mOutputDatasQueue.poll();
    }

    // 启动编码器
    public void startEncoder() {
        if (mMediaCodec != null) {
            // 设置编码器的回调监听
            mMediaCodec.setCallback(mCallback);
            // 配置MediaCodec，选择采用编码器功能
            mMediaCodec.configure(mMediaFormat, null, null, CONFIGURE_FLAG_ENCODE);
            // 启动编码器
            mMediaCodec.start();
        } else {
            throw new IllegalArgumentException("startEncoder failed,is the MediaCodec has been init correct?");
        }
    }

    // 停止编码器
    public void stopEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
        }
    }

    // 释放编码器
    public void releaseEncoder() {
        if (mMediaCodec != null) {
            mInputDatasQueue.clear();
            mOutputDatasQueue.clear();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
