package com.secomid.fotathird.fota;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.secomid.fotathird.FotaApplication;
import com.secomid.fotathird.fota.policy.PolicyInter;
import com.secomid.fotathird.fota.policy.PolicyManager;
import com.secomid.fotathird.utils.NetworkUtils;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.inter.ICheckVersionCallback;
import com.fota.iport.inter.IOnDownloadListener;
import com.fota.iport.service.DLService;
import com.fota.utils.Trace;

import java.io.File;

/**
 * 检测版本，下载，升级服务类
 * Created by raise.yang on 2016/07/15.
 */
public class FotaIntentService extends IntentService {

    private static final java.lang.String TAG = "FotaIntentService";
    public static final String KEY_SERVICE_TYPE = "key_service_type";
    // 分别启动3类服务：检测，下载，升级
    public static final int TYPE_CHECK = 1;
    public static final int TYPE_DOWNLOAD = 2;
    public static final int TYPE_UPGRADE = 3;
    public static PolicyInter s_policyInter = new PolicyManager();

    private static Context m_context;

    public FotaIntentService() {
        super("FotaIntentService");
    }

    public static void startService(Context context, int type) {
        m_context = context.getApplicationContext();
        //启动服务
        Intent intent = new Intent(context, FotaIntentService.class);
        intent.putExtra(KEY_SERVICE_TYPE, type);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            dispatchServiceType(intent.getIntExtra(KEY_SERVICE_TYPE, -1));
        }
    }

    private void dispatchServiceType(int type) {
        switch (type) {
            case TYPE_CHECK:
                check_version();
                break;
            case TYPE_DOWNLOAD:
                download_file();
                break;
            case TYPE_UPGRADE:
                recovery_upgrade();
                break;
        }
    }

    /**
     * SDK 检测版本接口
     */
    private void check_version() {

            MobAgentPolicy.checkVersion(m_context, new ICheckVersionCallback() {
                @Override
                public void onCheckSuccess(int status) {
                    Trace.d(TAG, "onCheckSuccess() [status] " + status);
                    sendLocalBroadcast(LocalReceiver.ACTION_CHECK_SUCCESS, null);
                    boolean auto_upgrade = s_policyInter.is_auto_upgrade();
                    Trace.d(TAG, "onCheckSuccess() is_auto_upgrade() = " + auto_upgrade);
                    if (auto_upgrade) {
                        //后台配置了强制升级-开始下载
                        FotaIntentService.startService(m_context, TYPE_DOWNLOAD);
                    } else {
                        //后台没配置强制升级-通知栏
                        if (s_policyInter.is_notify_pop()) {
                            //flag
                        } else {
                            show_notification(getString(com.secomid.fotathird.R.string.has_new_version));
                        }
                    }
                }

                @Override
                public void onCheckFail(int status, String errorMsg) {
                    Trace.d(TAG, "onCheckFail() [status, errorMsg] " + errorMsg);
                    sendLocalBroadcast(LocalReceiver.ACTION_CHECK_ERROR, null);
                }

                @Override
                public void onInvalidDate() {
                    Trace.d(TAG, "onInvalidDate()");
                    sendLocalBroadcast(LocalReceiver.ACTION_CHECK_ERROR, "no network or response data is exception.");
                }
            });

    }

    /**
     * 下载升级包接口
     */
    private void download_file() {
        if (s_policyInter.is_request_wifi()) {
            if (!NetworkUtils.isWifi(m_context)) {//中文
                sendLocalBroadcast(LocalReceiver.ACTION_DOWNLOAD_ERROR, "配置了wifi网络下载，但不在wifi下");
                return;
            }
        }

        File down_path = new File(FotaApplication.s_update_package_absolute_path);
        //获取后台配置路径
        String configPath = s_policyInter.get_storage_path();
        if (!TextUtils.isEmpty(configPath)) {
            down_path = new File(configPath);
        }
        // 检查内存空间
        if (!s_policyInter.is_storage_space_enough(down_path.getAbsolutePath())) {
            sendLocalBroadcast(LocalReceiver.ACTION_DOWNLOAD_ERROR, "配置了最小内存空间，但空间不足");
            return;
        }


        //启动SDK下载接口
        DLService.start(m_context,
                MobAgentPolicy.getVersionInfo().deltaUrl,//下载url
                new File(down_path.getParent()),// 文件父目录
                down_path.getName(),// 文件名
                new IOnDownloadListener() {
                    @Override
                    public void onDownloadProgress(String tmpPath, int totalSize, int downloadedSize) {
//                        Trace.d(TAG, "onDownloadProgress() [tmpPath, totalSize, downloadedSize] " + totalSize + "," + downloadedSize);
                        //发送广播给主界面通知下载进度
                        sendLocalBroadcast(LocalReceiver.ACTION_DOWNLOAD_PROGRESS, String.valueOf(downloadedSize * 100 / totalSize));
                    }

                    @Override
                    public void onDownloadFinished(int state, File file) {
                        Trace.d(TAG, "onDownloadFinished() [state, file] " + state);
                        sendLocalBroadcast(LocalReceiver.ACTION_DOWNLOAD_FINISHED, null);
                        if (s_policyInter.is_auto_upgrade()) {
                            Trace.d(TAG, "onDownloadFinished() auto upgrade");
                            FotaIntentService.startService(m_context, TYPE_UPGRADE);
                        } else {
                            Trace.d(TAG, "onDownloadFinished() not auto upgrade");
                        }
                    }

                    @Override
                    public void onDownloadError(int error) {
                        Trace.d(TAG, "onDownloadError() [error] " + error);
                        //根据错误码，对错误做出对应的处理
                        sendLocalBroadcast(LocalReceiver.ACTION_DOWNLOAD_ERROR, String.valueOf(error));
                    }
                }
        );
    }

    //广播通知UI界面更新
    private void sendLocalBroadcast(String action, String arg0) {
        LocalReceiver.sendReceiver(m_context, action, arg0);
    }

    /**
     * 升级接口
     */
    private void recovery_upgrade() {
        Trace.d(TAG, "recovery_upgrade() ");
        //升级开始
        sendLocalBroadcast(LocalReceiver.ACTION_UPGRADE_START, null);
        //电量检查
        if (!s_policyInter.is_battery_enough(m_context)) {
            sendLocalBroadcast(LocalReceiver.ACTION_UPGRADE_ERROR, "配置了最小电量，但电量不足");
            return;
        }

        File down_path = new File(FotaApplication.s_update_package_absolute_path);
        //获取后台配置路径
        String configPath = s_policyInter.get_storage_path();
        if (!TextUtils.isEmpty(configPath)) {
            down_path = new File(configPath);
        }
        try {
            MobAgentPolicy.rebootUpgrade(m_context, down_path.getAbsolutePath());
        } catch (RuntimeException e) {
            Trace.d(TAG, "recovery_upgrade() error.");
            e.printStackTrace();
            sendLocalBroadcast(LocalReceiver.ACTION_UPGRADE_ERROR, e.getMessage());
        }
    }

    private static final int CHECK_ID = 1;

    private void show_notification(String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(com.secomid.fotathird.R.drawable.ic_launcher)
                        .setContentTitle(getString(com.secomid.fotathird.R.string.remind))
                        .setContentText(msg);
        Intent resultIntent = new Intent(this, FotaActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

        mBuilder.setContentIntent(contentIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (s_policyInter.is_notification_always())
            mBuilder.setAutoCancel(false);
        mNotificationManager.notify(CHECK_ID, mBuilder.build());
    }
}
