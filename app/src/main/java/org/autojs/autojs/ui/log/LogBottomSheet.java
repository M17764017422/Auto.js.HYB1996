package org.autojs.autojs.ui.log;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.stardust.autojs.core.console.ConsoleImpl;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.BottomSheetLogBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bottom sheet dialog for displaying script logs in the editor
 * Provides quick access to log output without leaving the editor
 * Supports clicking on stack frames to jump to source lines
 */
public class LogBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SCRIPT_NAME = "script_name";
    private static final String ARG_SCRIPT_PATH = "script_path";

    // Pattern to match stack frame formats:
    // - at functionName(file:line)
    // - at file:line
    // - file:line:column
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "(?:at\\s+)?(?:(\\w+)\\s*\\([^)]*?:(\\d+)(?::(\\d+))?\\)|([^\\s():]+):(\\d+)(?::(\\d+))?)"
    );

    private BottomSheetLogBinding binding;
    private String mScriptName;
    private String mScriptPath;
    private OnStackFrameClickListener mStackFrameClickListener;
    private StackFrameAdapter mAdapter;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRefreshRunnable;
    private static final int REFRESH_INTERVAL = 500;

    public interface OnStackFrameClickListener {
        void onStackFrameClick(String fileName, int lineNumber, int columnNumber);
    }

    /**
     * Create a new instance with script info
     */
    public static LogBottomSheet newInstance(String scriptName, String scriptPath) {
        LogBottomSheet fragment = new LogBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SCRIPT_NAME, scriptName);
        args.putString(ARG_SCRIPT_PATH, scriptPath);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnStackFrameClickListener(OnStackFrameClickListener listener) {
        mStackFrameClickListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mScriptName = getArguments().getString(ARG_SCRIPT_NAME);
            mScriptPath = getArguments().getString(ARG_SCRIPT_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        // Set title
        if (mScriptName != null && !mScriptName.isEmpty()) {
            binding.tvTitle.setText(mScriptName);
        }

        // Setup ConsoleView with global console
        AutoJs autoJs = AutoJs.getInstance();
        if (autoJs != null) {
            binding.console.setConsole(autoJs.getGlobalConsole());
            binding.console.findViewById(R.id.input_container).setVisibility(View.GONE);
        }

        // Setup stack frames RecyclerView
        mAdapter = new StackFrameAdapter();
        binding.recyclerStackFrames.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStackFrames.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((frame, position) -> {
            if (mStackFrameClickListener != null) {
                mStackFrameClickListener.onStackFrameClick(
                        frame.fileName, 
                        frame.lineNumber - 1, // Convert to 0-based
                        frame.columnNumber
                );
                dismiss();
            }
        });

        // Clear button
        binding.btnClear.setOnClickListener(v -> {
            AutoJs instance = AutoJs.getInstance();
            if (instance != null) {
                instance.getGlobalConsole().clear();
            }
            updateStackFrames();
        });

        // Open full log activity button
        binding.btnOpenFull.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LogActivity.class));
            dismiss();
        });

        // Start periodic refresh of stack frames
        startRefreshTask();
    }

    private void startRefreshTask() {
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && binding != null) {
                    updateStackFrames();
                    mHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        mHandler.post(mRefreshRunnable);
    }

    private void updateStackFrames() {
        AutoJs autoJs = AutoJs.getInstance();
        if (autoJs == null || autoJs.getGlobalConsole() == null) return;

        ArrayList<ConsoleImpl.LogEntry> logs = autoJs.getGlobalConsole().getAllLogs();
        if (logs == null || logs.isEmpty()) {
            binding.stackFramesContainer.setVisibility(View.GONE);
            return;
        }

        // Get the last few log entries and parse for stack frames
        List<StackFrameInfo> allFrames = new ArrayList<>();
        Set<String> seenFrames = new HashSet<>(); // 用于去重
        int startIdx = Math.max(0, logs.size() - 20); // Check last 20 logs
        
        for (int i = startIdx; i < logs.size(); i++) {
            ConsoleImpl.LogEntry entry = logs.get(i);
            if (entry.content != null) {
                List<StackFrameInfo> frames = parseStackFrames(entry.content.toString());
                for (StackFrameInfo frame : frames) {
                    // 过滤无效行号
                    if (frame.lineNumber <= 0) continue;
                    
                    // 生成唯一键用于去重
                    String key = frame.fileName + ":" + frame.lineNumber + ":" + frame.functionName;
                    if (!seenFrames.contains(key)) {
                        seenFrames.add(key);
                        allFrames.add(frame);
                    }
                }
            }
        }

        if (allFrames.isEmpty()) {
            binding.stackFramesContainer.setVisibility(View.GONE);
        } else {
            binding.stackFramesContainer.setVisibility(View.VISIBLE);
            mAdapter.setFrames(allFrames);
        }
    }

    /**
     * Parse stack frame information from log content
     * @param content the log content to parse
     * @return list of StackFrameInfo found in the content
     */
    public static List<StackFrameInfo> parseStackFrames(String content) {
        List<StackFrameInfo> frames = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return frames;
        }

        Matcher matcher = STACK_FRAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String funcName;
            String fileName;
            int lineNum;
            int colNum = 0;

            if (matcher.group(1) != null) {
                // Format: at functionName(file:line) or at functionName(file:line:col)
                funcName = matcher.group(1);
                fileName = extractFileName(content, matcher.start());
                lineNum = Integer.parseInt(matcher.group(2));
                if (matcher.group(3) != null) {
                    colNum = Integer.parseInt(matcher.group(3));
                }
            } else {
                // Format: at file:line or file:line:col
                funcName = "<global>";
                fileName = matcher.group(4);
                lineNum = Integer.parseInt(matcher.group(5));
                if (matcher.group(6) != null) {
                    colNum = Integer.parseInt(matcher.group(6));
                }
            }

            // 只添加有有效行号的帧
            if (lineNum > 0) {
                frames.add(new StackFrameInfo(funcName, fileName, lineNum, colNum, matcher.start(), matcher.end()));
            }
        }

        return frames;
    }

    /**
     * Try to extract filename from context around the match
     */
    private static String extractFileName(String content, int matchStart) {
        // Look for filename in parentheses before the line number
        int parenStart = content.lastIndexOf('(', matchStart);
        if (parenStart > 0) {
            int parenEnd = content.indexOf(':', parenStart);
            if (parenEnd > parenStart) {
                return content.substring(parenStart + 1, parenEnd).trim();
            }
        }
        return "script";
    }

    /**
     * Represents a parsed stack frame from log content
     */
    public static class StackFrameInfo {
        public final String functionName;
        public final String fileName;
        public final int lineNumber;
        public final int columnNumber;
        public final int startIndex;
        public final int endIndex;

        public StackFrameInfo(String functionName, String fileName, int lineNumber, int columnNumber, int startIndex, int endIndex) {
            this.functionName = functionName;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    @Override
    public void onDestroyView() {
        if (mHandler != null && mRefreshRunnable != null) {
            mHandler.removeCallbacks(mRefreshRunnable);
        }
        super.onDestroyView();
        binding = null;
    }
}