package com.stardust.autojs.project;

import com.stardust.autojs.ScriptEngineService;
import com.stardust.autojs.execution.ExecutionConfig;
import com.stardust.autojs.script.JavaScriptFileSource;
import com.stardust.pio.PFiles;

import java.io.File;

public class ProjectLauncher {

    private String mProjectDir;
    private String mMainScriptPath;
    private ProjectConfig mProjectConfig;

    public ProjectLauncher(String projectDir) {
        mProjectDir = projectDir;
        mProjectConfig = ProjectConfig.fromProjectDir(projectDir);
        // 使用 PFiles.join 支持 SAF 路径
        mMainScriptPath = PFiles.join(mProjectDir, mProjectConfig.getMainScriptFile());
    }

    public void launch(ScriptEngineService service) {
        ExecutionConfig config = new ExecutionConfig();
        config.setWorkingDirectory(mProjectDir);
        config.getScriptConfig().setFeatures(mProjectConfig.getFeatures());
        service.execute(new JavaScriptFileSource(mMainScriptPath), config);
    }

}
