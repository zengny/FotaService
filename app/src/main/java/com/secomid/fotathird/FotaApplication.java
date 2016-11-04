package com.secomid.fotathird;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.secomid.fotathird.fota.policy.PolicyConfig;
import com.secomid.fotathird.utils.Constant;
import com.secomid.fotathird.utils.StorageUtils;
import com.device.utils.Mid;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.SPFTool;
import com.fota.iport.info.MobileParamInfo;
import com.fota.utils.Trace;

/**
 * Created by raise.yang on 2016/05/06.
 */
public class FotaApplication extends Application {


    public static String s_update_package_absolute_path;

    public static Context s_context;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        s_context = base;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // sdk打印日志功能
        Trace.setLevel(Trace.DEBUG);//打开日志
//        Trace.setLevel(Trace.NONE);//关闭日志
//        Trace.setLog_path(getExternalCacheDir() + "/iport_log.txt");//设置log存储路径,默认在内置存储根目录下iport_log.txt文件
//        下载文件保存路径
        s_update_package_absolute_path = StorageUtils.get_update_file_path(this);

        //init mid. sure the mid is unique//写文件形式
        String mid = SPFTool.getString(this, "mid", null);
        if (mid == null) {
            mid = Mid.getMid(this);
            SPFTool.putString(this, "mid", mid);
        }
        //使用imei作为设备mid 需要在manifest.xml文件中要添加 <uses-permission android:name="android.permission.READ_PHONE_STATE" />
//        String mid = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
//                .getDeviceId();

        //init mobile parameter
        try {
//            手机设备-需要fae在手机中配置build.prop文件
//            MobileParamInfo.initInfo(this, mid);
            //智能设备
//            String version = SPFTool.getString(this, SPFTool.KEY_WEAR_DEVICES_VERSION, Constant.version);
            MobileParamInfo.initInfo(this, mid, Constant.version, Constant.oem, Constant.models, Constant.token, Constant.platform, Constant.deviceType);
//            MobileParamInfo.initInfo(this, mid);
        } catch (Exception e) {
            Log.d("Tag", "MobileParamsInfo = " + MobileParamInfo.getInstance().toString());
            Trace.e("FotaApplication", e.toString());
            //ignore
        }
        //配置后台策略信息-默认所有策略启用
        PolicyConfig.getInstance()
                .request_wifi(true)//下载只能在wifi网络
                .request_battery(true)//升级时，电量最小值
                .request_check_cycle(true)//检测版本周期
                .request_install_force(false)//强制升级
                .request_notification(true)//检测到新版本时，提醒方式
                .request_storage_path(true)//升级包下载路径
                .request_self_update(false)//自更新默认关闭
                .request_storage_size(true);//本地存储最小空间 单位Byte

        // 初始化sdk缓存信息
        MobAgentPolicy.initConfig(this);

    }
}
