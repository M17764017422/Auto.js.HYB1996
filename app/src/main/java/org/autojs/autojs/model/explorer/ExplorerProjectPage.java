package org.autojs.autojs.model.explorer;

import com.stardust.autojs.project.ProjectConfig;
import com.stardust.pio.PFile;
import com.stardust.pio.PFiles;

import java.io.File;

public class ExplorerProjectPage extends ExplorerDirPage {

    private ProjectConfig mProjectConfig;

    public ExplorerProjectPage(PFile file, ExplorerPage parent, ProjectConfig projectConfig) {
        super(file, parent);
        mProjectConfig = projectConfig;
    }

    public ExplorerProjectPage(String path, ExplorerPage parent, ProjectConfig projectConfig) {
        super(path, parent);
        mProjectConfig = projectConfig;
    }

    public ExplorerProjectPage(File file, ExplorerPage parent, ProjectConfig projectConfig) {
        super(file, parent);
        mProjectConfig = projectConfig;
    }

    public ProjectConfig getProjectConfig() {
        return mProjectConfig;
    }

    @Override
    public ExplorerFileItem rename(String newName) {
        String newPath = new File(getFile().getParent(), newName).getPath();
        if (PFiles.rename(getFile().getPath(), newName)) {
            return new ExplorerProjectPage(newPath, getParent(), mProjectConfig);
        }
        // 重命名失败，返回原对象
        return this;
    }
}
