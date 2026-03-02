package org.autojs.autojs.model.project;

import android.annotation.SuppressLint;

import com.stardust.autojs.project.ProjectConfig;
import com.stardust.pio.PFiles;
import com.stardust.pio.IFileProvider;

import org.autojs.autojs.storage.FileProviderFactory;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProjectTemplate {


    private final ProjectConfig mProjectConfig;
    private final File mProjectDir;

    public ProjectTemplate(ProjectConfig projectConfig, File projectDir) {
        mProjectConfig = projectConfig;
        mProjectDir = projectDir;
    }

    @SuppressLint("CheckResult")
    public Observable<File> newProject() {
        return Observable.fromCallable(() -> {
            mProjectDir.mkdirs();
            String configPath = ProjectConfig.configFileOfDir(mProjectDir.getPath());
            String configJson = mProjectConfig.toJson();
            
            // 使用 FileProvider API 以支持 SAF 模式
            IFileProvider fileProvider = FileProviderFactory.getProvider();
            if (fileProvider != null) {
                // 写入配置文件
                fileProvider.write(configPath, configJson);
                // 创建主脚本文件
                String mainScriptPath = mProjectDir.getPath() + "/" + mProjectConfig.getMainScriptFile();
                fileProvider.write(mainScriptPath, "");
            } else {
                // 降级到传统方式
                PFiles.write(configPath, configJson);
                new File(mProjectDir, mProjectConfig.getMainScriptFile()).createNewFile();
            }
            return mProjectDir;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
