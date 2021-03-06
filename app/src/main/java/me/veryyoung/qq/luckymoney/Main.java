package me.veryyoung.qq.luckymoney;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;


public class Main implements IXposedHookLoadPackage {

    private static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";

    static long msgUid;
    static String senderuin;
    static String frienduin;
    static int istroop;
    static String selfuin;
    static Context globalContext = null;
    static Object HotChatManager = null;
    static Object TicketManager;
    static Object FriendsManager;

    private void dohook(final XC_LoadPackage.LoadPackageParam loadPackageParam) {


        findAndHookMethod("com.tencent.mobileqq.app.MessageHandlerUtils", loadPackageParam.classLoader, "a",
                "com.tencent.mobileqq.app.QQAppInterface",
                "com.tencent.mobileqq.data.MessageRecord", Boolean.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open()) {
                            return;
                        }
                        int msgtype = (int) getObjectField(param.args[1], "msgtype");
                        if (msgtype == -2025) {
                            msgUid = (long) getObjectField(param.args[1], "msgUid");
                            senderuin = (String) getObjectField(param.args[1], "senderuin");
                            frienduin = getObjectField(param.args[1], "frienduin").toString();
                            istroop = (int) getObjectField(param.args[1], "istroop");
                            selfuin = getObjectField(param.args[1], "selfuin").toString();
                        }
                    }
                }

        );

        findAndHookMethod("com.tencent.mobileqq.data.MessageForQQWalletMsg", loadPackageParam.classLoader, "doParse", new
                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open() || msgUid == 0) {
                            return;
                        }
                        msgUid = 0;

                        int messageType = (int) XposedHelpers.getObjectField(param.thisObject, "messageType");
                        if (messageType == 6 && !PreferencesUtils.password()) {
                            return;
                        }


                        Object mQQWalletRedPacketMsg = getObjectField(param.thisObject, "mQQWalletRedPacketMsg");
                        String redPacketId = getObjectField(mQQWalletRedPacketMsg, "redPacketId").toString();
                        String authkey = (String) getObjectField(mQQWalletRedPacketMsg, "authkey");
                        ClassLoader walletClassLoader = (ClassLoader) callStaticMethod(findClass("com.tencent.mobileqq.pluginsdk.PluginStatic", loadPackageParam.classLoader), "getOrCreateClassLoader", globalContext, "qwallet_plugin.apk");
                        StringBuffer requestUrl = new StringBuffer();
                        requestUrl.append("&uin=" + selfuin);
                        requestUrl.append("&listid=" + redPacketId);
                        requestUrl.append("&name=" + Uri.encode(""));
                        requestUrl.append("&answer=");
                        if (istroop == 0) {
                            requestUrl.append("&groupid=" + selfuin);
                        } else {
                            requestUrl.append("&groupid=" + frienduin);
                        }
                        requestUrl.append("&grouptype=" + getGroupType());
                        requestUrl.append("&groupuin=" + senderuin);
                        requestUrl.append("&authkey=" + authkey);

                        Class findClass = findClass("com.tenpay.android.qqplugin.a.p", walletClassLoader);

                        int random = Math.abs(new Random().nextInt()) % 16;
                        String reqText = (String) callStaticMethod(findClass, "a", globalContext, Integer.valueOf(random), Boolean.valueOf(false), requestUrl.toString());
                        StringBuffer hongbaoRequestUrl = new StringBuffer();
                        hongbaoRequestUrl.append("https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3");
                        hongbaoRequestUrl.append("&req_text=" + reqText);
                        hongbaoRequestUrl.append("&random=" + random);
                        hongbaoRequestUrl.append("&skey_type=2");
                        hongbaoRequestUrl.append("&skey=" + callMethod(TicketManager, "getSkey", selfuin));

                        Object pickObject = XposedHelpers.newInstance(findClass("com.tenpay.android.qqplugin.b.d", walletClassLoader), callStaticMethod(findClass, "a", globalContext));
                        Bundle bundle = (Bundle) callMethod(pickObject, "a", hongbaoRequestUrl.toString());
                        String pickKey = (String) callStaticMethod(findClass, "a", globalContext, Integer.valueOf(random), callStaticMethod(findClass, "a", globalContext, bundle, new JSONObject()));

                        if (PreferencesUtils.delay()) {
                            Thread.sleep(PreferencesUtils.delayTime());
                        }
                        callStaticMethod(findClass, "a", Integer.valueOf(random), pickKey);
                    }
                }

        );


        findAndHookMethod("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader, "doOnCreate", Bundle.class, new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        globalContext = (Context) param.thisObject;
                    }
                }

        );


        findAndHookConstructor("mqq.app.TicketManagerImpl", loadPackageParam.classLoader, "mqq.app.AppRuntime", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TicketManager = param.thisObject;
            }
        });


        findAndHookConstructor("com.tencent.mobileqq.app.HotChatManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HotChatManager = param.thisObject;
                    }
                }

        );

        findAndHookMethod("com.tencent.mobileqq.pluginsdk.PluginProxyActivity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Intent intent = (Intent) callMethod(param.thisObject, "getIntent");
                        ClassLoader classLoader = (ClassLoader) callStaticMethod(findClass("com.tencent.mobileqq.pluginsdk.PluginStatic", loadPackageParam.classLoader), "a", param.thisObject, getObjectField(param.thisObject, "k").toString(), getObjectField(param.thisObject, "i").toString());
                        if (intent.getStringExtra("pluginsdk_launchActivity").equals("com.tenpay.android.qqplugin.activity.GrapHbActivity")) {
                            findAndHookMethod("com.tenpay.android.qqplugin.activity.GrapHbActivity", classLoader, "a", JSONObject.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            Object obj = getObjectField(param.thisObject, "mCloseBtn");
                                            callMethod(param.thisObject, "finish");
                                            callMethod(obj, "performClick");
                                        }
                                    });
                        }
                    }
                }

        );


        findAndHookMethod("com.tencent.mobileqq.activity.aio.item.QQWalletMsgItemBuilder", loadPackageParam.classLoader, "a", "mbw", "com.tencent.mobileqq.data.MessageForQQWalletMsg", "com.tencent.mobileqq.activity.aio.OnLongClickAndTouchListener",
                new XC_MethodHook() {
                    int issend;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        issend = (int) getObjectField(param.args[1], "issend");
                        if (issend != 1) {
                            setObjectField(param.args[1], "issend", 1);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.args[1], "issend", issend);
                    }
                }

        );
    }


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (loadPackageParam.packageName.equals(QQ_PACKAGE_NAME)) {
            hideModule(loadPackageParam);

            int ver = Build.VERSION.SDK_INT;
            if (ver < 21) {
                findAndHookMethod("com.tencent.common.app.BaseApplicationImpl", loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        dohook(loadPackageParam);
                    }
                });
            } else {
                dohook(loadPackageParam);
            }
        }

    }

    private void hideModule(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<ApplicationInfo> applicationList = (List) param.getResult();
                List<ApplicationInfo> resultapplicationList = new ArrayList<>();
                for (ApplicationInfo applicationInfo : applicationList) {
                    if (!applicationInfo.processName.contains("veryyoung")) {
                        resultapplicationList.add(applicationInfo);
                    }
                }
                param.setResult(resultapplicationList);
            }
        });
        findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<PackageInfo> packageInfoList = (List) param.getResult();
                List<PackageInfo> resultpackageInfoList = new ArrayList<>();
                for (PackageInfo packageInfo : packageInfoList) {
                    if (!packageInfo.packageName.contains("veryyoung")) {
                        resultpackageInfoList.add(packageInfo);
                    }
                }
                param.setResult(resultpackageInfoList);
            }
        });
    }


    private int getGroupType() throws IllegalAccessException {
        int grouptype = 0;
        if (istroop == 3000) {
            grouptype = 2;

        } else if (istroop == 1) {
            Map map = (Map) findFirstFieldByExactType(HotChatManager.getClass(), Map.class).get(HotChatManager);
            if (map != null & map.containsKey(frienduin)) {
                grouptype = 5;
            } else {
                grouptype = 1;
            }
        } else if (istroop == 0) {
            grouptype = 0;
        } else if (istroop == 1004) {
            grouptype = 4;

        } else if (istroop == 1000) {
            grouptype = 3;

        } else if (istroop == 1001) {
            grouptype = 6;
        }
        return grouptype;
    }


}