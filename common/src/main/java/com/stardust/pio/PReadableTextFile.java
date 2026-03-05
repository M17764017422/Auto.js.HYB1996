package com.stardust.pio;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stardust on 2017/4/1.
 */

public class PReadableTextFile implements Closeable, PFileInterface {

    private BufferedReader mBufferedReader;
    private InputStream mInputStream;
    private int mBufferingSize;
    private String mEncoding;
    private String mPath;
    private boolean mIsStreamMode;

    public PReadableTextFile(String path) {
        this(path, PFiles.DEFAULT_ENCODING);
    }

    public PReadableTextFile(String path, String encoding) {
        this(path, encoding, -1);
    }

    public PReadableTextFile(String path, String encoding, int bufferingSize) {
        mEncoding = encoding;
        mBufferingSize = bufferingSize;
        mPath = path;
        mIsStreamMode = false;
        try {
            mInputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从 InputStream 创建读取器（用于 SAF 模式）
     */
    public PReadableTextFile(InputStream inputStream, String encoding, int bufferingSize) {
        mEncoding = encoding;
        mBufferingSize = bufferingSize;
        mPath = null;
        mIsStreamMode = true;
        mInputStream = inputStream;
    }

    /**
     * 从 InputStream 创建读取器（用于 SAF 模式）
     */
    public PReadableTextFile(InputStream inputStream, String encoding) {
        this(inputStream, encoding, -1);
    }

    /**
     * 从 InputStream 创建读取器（用于 SAF 模式）
     */
    public PReadableTextFile(InputStream inputStream) {
        this(inputStream, PFiles.DEFAULT_ENCODING, -1);
    }


    private void ensureBufferReader() {
        if (mBufferedReader == null) {
            try {
                if (mBufferingSize == -1)
                    mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream, mEncoding));
                else
                    mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream, mEncoding), mBufferingSize);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

        }
    }

    public String read() {
        ensureBufferReader();
        try {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = mBufferedReader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String read(int size) {
        ensureBufferReader();
        try {
            char[] chars = new char[size];
            int len = mBufferedReader.read(chars);
            return new String(chars, 0, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String readline() {
        ensureBufferReader();
        try {
            return mBufferedReader.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String[] readlines() {
        ensureBufferReader();
        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = mBufferedReader.readLine()) != null) {
                lines.add(line);
            }
            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (mBufferedReader != null) {
                mBufferedReader.close();
            } else if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getPath() {
        return mPath;
    }
}
