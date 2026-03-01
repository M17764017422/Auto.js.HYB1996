package org.autojs.autojs.model.explorer;

import android.net.Uri;

import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFile;

import org.autojs.autojs.storage.FileProviderFactory;

import java.io.FileFilter;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class ExplorerFileProvider implements ExplorerProvider {

    private final FileFilter mFileFilter;

    public ExplorerFileProvider(FileFilter fileFilter) {
        mFileFilter = fileFilter;
    }

    public ExplorerFileProvider() {
        this(null);
    }

    @Override
    public Single<? extends ExplorerPage> getExplorerPage(ExplorerPage page) {
        ExplorerPage parent = page.getParent();
        String path = page.getPath();
        return listFiles(path)
                .collectInto(createExplorerPage(path, parent), (p, file) -> {
                    if(file.isDirectory()){
                        p.addChild(new ExplorerDirPage(file, p));
                    }else {
                        p.addChild(new ExplorerFileItem(file, p));
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    protected ExplorerDirPage createExplorerPage(String path, ExplorerPage parent) {
        return new ExplorerDirPage(path, parent);
    }

    /**
     * 列出目录文件，支持传统 File API 和 SAF
     */
    protected Observable<PFile> listFiles(String directoryPath) {
        return Observable.fromCallable(() -> {
            IFileProvider provider = FileProviderFactory.getProvider();
            List<IFileProvider.FileInfo> files = provider.listFiles(directoryPath);
            return files;
        })
        .flatMap(files -> Observable.fromIterable(files))
        .map(fileInfo -> {
            PFile file = new PFile(fileInfo.path);
            return file;
        });
    }

    /**
     * 旧方法保留兼容性
     */
    protected Observable<PFile> listFiles(PFile directory) {
        return listFiles(directory.getPath());
    }
}
