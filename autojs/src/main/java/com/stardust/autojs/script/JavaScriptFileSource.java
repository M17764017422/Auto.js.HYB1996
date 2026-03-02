package com.stardust.autojs.script;

import androidx.annotation.NonNull;

import com.stardust.pio.PFiles;
import com.stardust.pio.ScriptFileReader;
import com.stardust.pio.ScriptFileReaderRegistry;
import com.stardust.pio.UncheckedIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by Stardust on 2017/4/2.
 */

public class JavaScriptFileSource extends JavaScriptSource {

    private File mFile;
    private String mScript;
    private boolean mCustomsName = false;

    public JavaScriptFileSource(File file) {
        super(PFiles.getNameWithoutExtension(file.getName()));
        mFile = file;
    }

    public JavaScriptFileSource(String path) {
        this(new File(path));
    }

    public JavaScriptFileSource(String name, File file) {
        super(name);
        mCustomsName = true;
        mFile = file;
    }

    @NonNull
    @Override
    public String getScript() {
        if (mScript == null) {
            String filePath = mFile.getAbsolutePath();
            ScriptFileReader reader = ScriptFileReaderRegistry.get();
            if (reader != null) {
                try {
                    mScript = reader.read(filePath);
                } catch (Exception e) {
                    // 如果注册的读取器失败，尝试使用传统方式
                    mScript = PFiles.read(mFile);
                }
            } else {
                // 未注册读取器，使用传统方式
                mScript = PFiles.read(mFile);
            }
        }
        return mScript;
    }

    @Override
    protected int parseExecutionMode() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mFile);
            short flags = EncryptedScriptFileHeader.INSTANCE.getHeaderFlags(fis);
            if (flags == EncryptedScriptFileHeader.FLAG_INVALID_FILE) {
                return super.parseExecutionMode();
            }
            return flags;
        } catch (Exception e) {
            return super.parseExecutionMode();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public Reader getScriptReader() {
        String filePath = mFile.getAbsolutePath();
        ScriptFileReader reader = ScriptFileReaderRegistry.get();
        if (reader != null) {
            try {
                // 获取 InputStream 并包装为 InputStreamReader
                return new java.io.InputStreamReader(reader.openInputStream(filePath), "utf-8");
            } catch (Exception e) {
                // 如果注册的读取器失败，尝试使用传统方式
                try {
                    return new FileReader(mFile);
                } catch (FileNotFoundException e2) {
                    throw new UncheckedIOException(e2);
                }
            }
        } else {
            // 未注册读取器，使用传统方式
            try {
                return new FileReader(mFile);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public File getFile() {
        return mFile;
    }

    @Override
    public String toString() {
        if (mCustomsName) {
            return super.toString();
        }
        return mFile.toString();
    }
}
