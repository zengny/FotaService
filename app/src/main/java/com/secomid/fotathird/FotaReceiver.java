package com.secomid.fotathird;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.secomid.fotathird.fota.FotaIntentService;
import com.secomid.fotathird.fota.policy.PolicyInter;
import com.secomid.fotathird.fota.policy.PolicyManager;
import com.secomid.fotathird.update.UpdateService;
import com.secomid.fotathird.utils.NetworkUtils;
import com.fota.iport.Const;
import com.fota.iport.SPFTool;
import com.fota.utils.Trace;

/**
 * support check version in cycle.<br/>
 * <p/>
 * listening the action of {@link Const#ACTION_IPORT_RECOVERY_UPGRADE} to get the flag of upgrade
 */
public class FotaReceiver extends BroadcastReceiver {

    private static final String TAG = "FotaReceiver";
    private Context mCtx;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Trace.d(TAG, "action = " + action);
        mCtx = context;
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            network_process();
        } else if (action.equalsIgnoreCase(Const.ACTION_IPORT_RECOVERY_UPGRADE)) {
            // the flag of upgrade, will return true if success
            boolean is_upgrade = intent.getBooleanExtra(Const.KEY_UPGRADE_SUCCESS, false);
            upgrade_process(is_upgrade);
        }
    }

    private void upgrade_process(boolean is_upgrade) {
        if (is_upgrade) {
            Trace.d(TAG, "recovery upgrade success");
        } else {
            Trace.d(TAG, "recovery upgrade failed");
        }
    }

    private void network_process() {
        if (NetworkUtils.isNetworkAvailable(mCtx)) {
            //启动自升级服务
            UpdateService.startService(mCtx);
            //读取上次周期检测版本时间
            long previous_time = SPFTool.getLong(mCtx, "key_cycle_check", 0);
            long cur_time = System.currentTimeMillis();
            Trace.d(TAG, String.format("check version in cycle. gap time = %s min", (cur_time - previous_time) / 60 / 1000));
            PolicyInter policyInter = new PolicyManager();
            if (policyInter.get_check_cycle() > 0
                    && (cur_time - previous_time) / 60 / 1000 > policyInter.get_check_cycle()) {
                //fota后台配有检测版本周期，注默认后台配置为1440分钟20160716
                //根据上次检测时间和当前时间差值判断周期已经达到
                //start service of check version
                FotaIntentService.startService(mCtx, FotaIntentService.TYPE_CHECK);
                //更新检测时间
                SPFTool.putLong(mCtx, "key_cycle_check", cur_time);
            }
        }
    }
}
