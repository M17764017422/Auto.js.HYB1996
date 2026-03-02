package com.stardust.autojs.project;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Stardust on 2018/1/24.
 */

public class ProjectConfig {

    public static final String CONFIG_FILE_NAME = "project.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 自定义文件提供者，用于 SAF 模式下读取配置文件
     * 由 app 模块的 FileProviderFactory 设置
     */
    private static IFileProvider sFileProvider;

    /**
     * 设置文件提供者
     */
    public static void setFileProvider(IFileProvider provider) {
        sFileProvider = provider;
    }
    
    /**
     * 获取当前文件提供者
     */
    public static IFileProvider getFileProvider() {
        return sFileProvider;
    }

    @SerializedName("name")
    private String mName;

    @SerializedName("versionName")
    private String mVersionName;

    @SerializedName("versionCode")
    private int mVersionCode = -1;

    @SerializedName("packageName")
    private String mPackageName;

    @SerializedName("main")
    private String mMainScriptFile;

    @SerializedName("assets")
    private List<String> mAssets = new ArrayList<>();

    @SerializedName("launchConfig")
    private LaunchConfig mLaunchConfig;

    @SerializedName("build")
    private BuildInfo mBuildInfo = new BuildInfo();

    @SerializedName("icon")
    private String mIcon;

    @SerializedName("scripts")
    private Map<String, ScriptConfig> mScriptConfigs = new HashMap<>();

    @SerializedName("useFeatures")
    private List<String> mFeatures = new ArrayList<>();


    public static ProjectConfig fromJson(String json) {
        if (json == null) {
            return null;
        }
        ProjectConfig config = GSON.fromJson(json, ProjectConfig.class);
        if (!isValid(config)) {
            return null;
        }
        return config;
    }

    private static boolean isValid(ProjectConfig config) {
        if (TextUtils.isEmpty(config.getName())) {
            return false;
        }
        if (TextUtils.isEmpty(config.getPackageName())) {
            return false;
        }
        if (TextUtils.isEmpty(config.getVersionName())) {
            return false;
        }
        if (TextUtils.isEmpty(config.getMainScriptFile())) {
            return false;
        }
        if (config.getVersionCode() == -1) {
            return false;
        }
        return true;
    }


    public static ProjectConfig fromAssets(Context context, String path) {
        try {
            return fromJson(PFiles.read(context.getAssets().open(path)));
        } catch (Exception e) {
            return null;
        }
    }

    public static ProjectConfig fromFile(String path) {
        try {
            String json;
            // 优先使用 IFileProvider (支持 SAF 模式)
            if (sFileProvider != null) {
                json = sFileProvider.read(path);
            } else {
                json = PFiles.read(path);
            }
            return fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static ProjectConfig fromProjectDir(String path) {
        return fromFile(configFileOfDir(path));
    }


    public static String configFileOfDir(String projectDir) {
        return PFiles.join(projectDir, CONFIG_FILE_NAME);
    }

    public BuildInfo getBuildInfo() {
        return mBuildInfo;
    }

    public void setBuildInfo(BuildInfo buildInfo) {
        mBuildInfo = buildInfo;
    }

    public String getName() {
        return mName;
    }

    public ProjectConfig setName(String name) {
        mName = name;
        return this;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public ProjectConfig setVersionName(String versionName) {
        mVersionName = versionName;
        return this;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public ProjectConfig setVersionCode(int versionCode) {
        mVersionCode = versionCode;
        return this;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public ProjectConfig setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public String getMainScriptFile() {
        return mMainScriptFile;
    }

    public ProjectConfig setMainScriptFile(String mainScriptFile) {
        mMainScriptFile = mainScriptFile;
        return this;
    }

    public Map<String, ScriptConfig> getScriptConfigs() {
        return mScriptConfigs;
    }

    public List<String> getAssets() {
        if (mAssets == null) {
            mAssets = Collections.emptyList();
        }
        return mAssets;
    }

    public boolean addAsset(String assetRelativePath) {
        if (mAssets == null) {
            mAssets = new ArrayList<>();
        }
        for (String asset : mAssets) {
            if (new File(asset).equals(new File(assetRelativePath))) {
                return false;
            }
        }
        mAssets.add(assetRelativePath);
        return true;
    }

    public void setAssets(List<String> assets) {
        mAssets = assets;
    }

    public LaunchConfig getLaunchConfig() {
        if (mLaunchConfig == null) {
            mLaunchConfig = new LaunchConfig();
        }
        return mLaunchConfig;
    }

    public void setLaunchConfig(LaunchConfig launchConfig) {
        mLaunchConfig = launchConfig;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        mIcon = icon;
    }

    public String getBuildDir() {
        return "build";
    }

    public List<String> getFeatures() {
        return mFeatures;
    }

    public void setFeatures(List<String> features) {
        mFeatures = features;
    }

    public ScriptConfig getScriptConfig(String path) {
        ScriptConfig config = mScriptConfigs.get(path);
        if (config == null) {
            config = new ScriptConfig();
        }
        if(mFeatures.isEmpty()){
            return config;
        }
        ArrayList<String> features = new ArrayList<>(config.getFeatures());
        for (String feature : mFeatures) {
            if (!features.contains(feature)) {
                features.add(feature);
            }
        }
        config.setFeatures(features);
        return config;
    }
}
