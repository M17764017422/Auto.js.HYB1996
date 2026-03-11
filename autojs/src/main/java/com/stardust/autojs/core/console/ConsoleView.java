package com.stardust.autojs.core.console;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stardust.enhancedfloaty.ResizableExpandableFloatyWindow;
import com.stardust.autojs.R;
import com.stardust.util.MapBuilder;
import com.stardust.util.SparseArrayEntries;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Stardust on 2017/5/2.
 * <p>
 * TODO: 优化为无锁形式
 */
public class ConsoleView extends FrameLayout implements ConsoleImpl.LogListener {

    private static final Map<Integer, Integer> ATTRS = new MapBuilder<Integer, Integer>()
            .put(R.styleable.ConsoleView_color_verbose, Log.VERBOSE)
            .put(R.styleable.ConsoleView_color_debug, Log.DEBUG)
            .put(R.styleable.ConsoleView_color_info, Log.INFO)
            .put(R.styleable.ConsoleView_color_warn, Log.WARN)
            .put(R.styleable.ConsoleView_color_error, Log.ERROR)
            .put(R.styleable.ConsoleView_color_assert, Log.ASSERT)
            .build();

    static final SparseArray<Integer> COLORS = new SparseArrayEntries<Integer>()
            .entry(Log.VERBOSE, 0xdfc0c0c0)
            .entry(Log.DEBUG, 0xdfffffff)
            .entry(Log.INFO, 0xff64dd17)
            .entry(Log.WARN, 0xff2962ff)
            .entry(Log.ERROR, 0xffd50000)
            .entry(Log.ASSERT, 0xffff534e)
            .sparseArray();

    // Stack frame pattern: file:line or file:line:column
    // Matches: script.js:15, main.js:10:5, etc.
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "([\\w\\-./]+\\.js):(\\d+)(?::(\\d+))?"
    );
    
    // Blue color for clickable stack frames
    private static final int LINK_COLOR = 0xFF2196F3;

    private static final int REFRESH_INTERVAL = 100;
    private SparseArray<Integer> mColors = COLORS.clone();
    private ConsoleImpl mConsole;
    private RecyclerView mLogListRecyclerView;
    private EditText mEditText;
    private ResizableExpandableFloatyWindow mWindow;
    private LinearLayout mInputContainer;
    private boolean mShouldStopRefresh = false;
    private ArrayList<ConsoleImpl.LogEntry> mLogEntries = new ArrayList<>();
    private OnStackFrameClickListener mStackFrameClickListener;

    /**
     * Listener for stack frame clicks
     */
    public interface OnStackFrameClickListener {
        void onStackFrameClick(String fileName, int lineNumber, int columnNumber);
    }

    public void setOnStackFrameClickListener(OnStackFrameClickListener listener) {
        mStackFrameClickListener = listener;
    }

    public ConsoleView(Context context) {
        super(context);
        init(null);
    }

    public ConsoleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ConsoleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public void setColors(SparseArray<Integer> colors) {
        mColors = colors;
    }

    private void init(AttributeSet attrs) {
        inflate(getContext(), R.layout.console_view, this);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ConsoleView);
            for (Map.Entry<Integer, Integer> attr : ATTRS.entrySet()) {
                int styleable = attr.getKey();
                int logLevel = attr.getValue();
                mColors.put(logLevel, typedArray.getColor(styleable, mColors.get(logLevel)));
            }
        }
        mLogListRecyclerView = findViewById(R.id.log_list);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mLogListRecyclerView.setLayoutManager(manager);
        mLogListRecyclerView.setAdapter(new Adapter());
        initEditText();
        initSubmitButton();
    }

    private void initSubmitButton() {
        final Button submit = findViewById(R.id.submit);
        submit.setOnClickListener(v -> {
            CharSequence input = mEditText.getText();
            submitInput(input);
        });
    }

    private void submitInput(CharSequence input) {
        if (android.text.TextUtils.isEmpty(input)) {
            return;
        }
        if (mConsole.submitInput(input)) {
            mEditText.setText("");
        }
    }

    private void initEditText() {
        mEditText = findViewById(R.id.input);
        mEditText.setFocusableInTouchMode(true);
        mInputContainer = findViewById(R.id.input_container);
        OnClickListener listener = v -> {
            if (mWindow != null) {
                mWindow.requestWindowFocus();
                mEditText.requestFocus();
            }
        };
        mEditText.setOnClickListener(listener);
        mInputContainer.setOnClickListener(listener);
    }

    public void setConsole(ConsoleImpl console) {
        mConsole = console;
        mConsole.setConsoleView(this);
    }

    @Override
    public void onNewLog(ConsoleImpl.LogEntry logEntry) {

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mShouldStopRefresh = false;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLog();
                if (!mShouldStopRefresh) {
                    postDelayed(this, REFRESH_INTERVAL);
                }
            }
        }, REFRESH_INTERVAL);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mShouldStopRefresh = true;
    }


    @Override
    public void onLogClear() {
        post(() -> {
            mLogEntries.clear();
            mLogListRecyclerView.getAdapter().notifyDataSetChanged();
        });
    }

    private void refreshLog() {
        if (mConsole == null)
            return;
        int oldSize = mLogEntries.size();
        ArrayList<ConsoleImpl.LogEntry> logEntries = mConsole.getAllLogs();
        synchronized (mConsole.getAllLogs()) {
            final int size = logEntries.size();
            if (size == 0) {
                return;
            }
            if (oldSize >= size) {
                return;
            }
            if (oldSize == 0) {
                mLogEntries.addAll(logEntries);
            } else {
                for (int i = oldSize; i < size; i++) {
                    mLogEntries.add(logEntries.get(i));
                }
            }
            mLogListRecyclerView.getAdapter().notifyItemRangeInserted(oldSize, size - 1);
            mLogListRecyclerView.scrollToPosition(size - 1);
        }
    }

    public void setWindow(ResizableExpandableFloatyWindow window) {
        mWindow = window;
    }

    public void showEditText() {
        post(() -> {
            mWindow.requestWindowFocus();
            //mInputContainer.setVisibility(VISIBLE);
            mEditText.requestFocus();
        });
    }

    /**
     * Parse log content and create clickable spans for stack frames
     */
    private CharSequence createClickableContent(CharSequence content, int baseColor) {
        String text = content.toString();
        SpannableString spannable = new SpannableString(text);
        
        Matcher matcher = STACK_FRAME_PATTERN.matcher(text);
        boolean hasLinks = false;
        
        while (matcher.find()) {
            hasLinks = true;
            final String fileName = matcher.group(1);
            final int lineNumber = Integer.parseInt(matcher.group(2));
            final int columnNumber = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            
            final int start = matcher.start();
            final int end = matcher.end();
            
            // Create clickable span
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (mStackFrameClickListener != null) {
                        mStackFrameClickListener.onStackFrameClick(fileName, lineNumber - 1, columnNumber);
                    }
                }
            };
            
            // Apply clickable span
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Apply blue color
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(LINK_COLOR);
            spannable.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return hasLinks ? spannable : content;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.console_view_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ConsoleImpl.LogEntry logEntry = mLogEntries.get(position);
            int baseColor = mColors.get(logEntry.level);
            
            // Create clickable content with stack frame links
            CharSequence content = createClickableContent(logEntry.content, baseColor);
            holder.textView.setText(content);
            holder.textView.setTextColor(baseColor);
        }

        @Override
        public int getItemCount() {
            return mLogEntries.size();
        }
    }
}