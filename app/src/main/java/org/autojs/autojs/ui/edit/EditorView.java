package org.autojs.autojs.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.stardust.autojs.engine.JavaScriptEngine;
import com.stardust.autojs.engine.ScriptEngine;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.pio.FileProviderFactory;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFiles;
import com.stardust.util.BackPressedHandler;
import com.stardust.util.Callback;
import com.stardust.util.ViewUtils;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.EditorViewBinding;
import org.autojs.autojs.model.autocomplete.AutoCompletion;
import org.autojs.autojs.model.autocomplete.CodeCompletion;
import org.autojs.autojs.model.autocomplete.CodeCompletions;
import org.autojs.autojs.model.autocomplete.Symbol;
import org.autojs.autojs.model.indices.Module;
import org.autojs.autojs.model.indices.Property;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.ui.doc.ManualDialog;
import org.autojs.autojs.ui.edit.completion.CodeCompletionBar;
import org.autojs.autojs.ui.edit.debug.DebugBar;
import org.autojs.autojs.ui.edit.editor.CodeEditor;
import org.autojs.autojs.ui.edit.keyboard.FunctionsKeyboardHelper;
import org.autojs.autojs.ui.edit.keyboard.FunctionsKeyboardView;
import org.autojs.autojs.ui.edit.theme.Theme;
import org.autojs.autojs.ui.edit.theme.Themes;
import org.autojs.autojs.ui.edit.toolbar.DebugToolbarFragment;
import org.autojs.autojs.ui.edit.toolbar.NormalToolbarFragment;
import org.autojs.autojs.ui.edit.toolbar.SearchToolbarFragment;
import org.autojs.autojs.ui.edit.toolbar.ToolbarFragment;
import org.autojs.autojs.ui.log.LogActivity;
import org.autojs.autojs.ui.widget.EWebView;
import org.autojs.autojs.ui.widget.SimpleTextWatcher;

import java.io.File;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static org.autojs.autojs.model.script.Scripts.ACTION_ON_EXECUTION_FINISHED;
import static org.autojs.autojs.model.script.Scripts.EXTRA_EXCEPTION_COLUMN_NUMBER;
import static org.autojs.autojs.model.script.Scripts.EXTRA_EXCEPTION_LINE_NUMBER;
import static org.autojs.autojs.model.script.Scripts.EXTRA_EXCEPTION_MESSAGE;
import static org.autojs.autojs.model.script.Scripts.EXTRA_STACK_TRACE;

/**
 * Created by Stardust on 2017/9/28.
 */
public class EditorView extends FrameLayout implements CodeCompletionBar.OnHintClickListener, FunctionsKeyboardView.ClickCallback, ToolbarFragment.OnMenuItemClickListener {

    /**
     * Callback interface for Material3 LogSheet integration
     */
    public interface LogPanelCallback {
        void onShowLogPanel();
    }

    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_READ_ONLY = "readOnly";
    public static final String EXTRA_SAVE_ENABLED = "saveEnabled";
    public static final String EXTRA_RUN_ENABLED = "runEnabled";

    private EditorViewBinding binding;

    private String mName;
    private Uri mUri;
    private boolean mReadOnly = false;
    private int mScriptExecutionId;
    private AutoCompletion mAutoCompletion;
    private Theme mEditorTheme;
    private FunctionsKeyboardHelper mFunctionsKeyboardHelper;
    private LogPanelCallback mLogPanelCallback;
    private BroadcastReceiver mOnRunFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_ON_EXECUTION_FINISHED.equals(intent.getAction())) {
                mScriptExecutionId = ScriptExecution.NO_ID;
                if (mDebugging) {
                    exitDebugging();
                }
                setMenuItemStatus(R.id.run, true);
                String msg = intent.getStringExtra(EXTRA_EXCEPTION_MESSAGE);
                int line = intent.getIntExtra(EXTRA_EXCEPTION_LINE_NUMBER, -1);
                int col = intent.getIntExtra(EXTRA_EXCEPTION_COLUMN_NUMBER, 0);
                String stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE);
                
                // 解析堆栈帧
                java.util.ArrayList<CodeEditor.StackFrame> frames = parseStackTrace(stackTrace, line, col);
                binding.editor.setStackFrames(frames);
                
                // 跳转到第一个错误位置
                if (line >= 1) {
                    binding.editor.jumpTo(line - 1, col);
                }
                if (msg != null) {
                    showErrorMessage(msg);
                }
            }
        }
    };

    // 解析 Rhino scriptStackTrace 格式的堆栈帧
    // 格式: "\tat functionName(filename:line)" 或 "\tat filename:line"
    private java.util.ArrayList<CodeEditor.StackFrame> parseStackTrace(String stackTrace, int mainLine, int mainCol) {
        java.util.ArrayList<CodeEditor.StackFrame> frames = new java.util.ArrayList<>();
        
        // 添加主错误行
        if (mainLine >= 1) {
            frames.add(new CodeEditor.StackFrame("<error>", mainLine - 1, mainCol));
        }
        
        if (stackTrace == null || stackTrace.isEmpty()) {
            return frames;
        }
        
        // 解析 Rhino scriptStackTrace 格式:
        // \tat level3(test_stack.js:5)
        // \tat level2(test_stack.js:10)
        // \tat test_stack.js:22
        java.util.regex.Pattern stackPattern = java.util.regex.Pattern.compile(
            "\\s+at\\s+(?:(\\w+)\\s*\\([^)]*?:(\\d+)\\)|([^\\s(]+):(\\d+))"
        );
        java.util.regex.Matcher stackMatcher = stackPattern.matcher(stackTrace);
        
        while (stackMatcher.find()) {
            String funcName;
            int lineNum;
            
            if (stackMatcher.group(1) != null) {
                // 格式: at functionName(file:line)
                funcName = stackMatcher.group(1);
                lineNum = Integer.parseInt(stackMatcher.group(2)) - 1;
            } else {
                // 格式: at file:line (全局调用)
                funcName = "<global>";
                lineNum = Integer.parseInt(stackMatcher.group(4)) - 1;
            }
            
            // 避免重复添加
            if (frames.isEmpty() || frames.get(frames.size() - 1).lineNumber != lineNum) {
                frames.add(new CodeEditor.StackFrame(funcName, lineNum, 0));
            }
        }
        
        return frames;
    }

    private SparseBooleanArray mMenuItemStatus = new SparseBooleanArray();
    private String mRestoredText;
    private NormalToolbarFragment mNormalToolbar = new NormalToolbarFragment();
    private boolean mDebugging = false;

    public EditorView(Context context) {
        super(context);
        init(context);
    }

    public EditorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        binding = EditorViewBinding.inflate(LayoutInflater.from(context), this);
        
        //setTheme(Theme.getDefault(getContext()));
        setUpEditor();
        setUpInputMethodEnhancedBar();
        setUpFunctionsKeyboard();
        setMenuItemStatus(R.id.save, false);
        binding.docs.getWebView().getSettings().setDisplayZoomControls(true);
        binding.docs.getWebView().loadUrl(Pref.getDocumentationUrl() + "index.html");
        Themes.getCurrent(getContext())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setTheme);
        initNormalToolbar();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().registerReceiver(mOnRunFinishedReceiver, new IntentFilter(ACTION_ON_EXECUTION_FINISHED));
        if (getContext() instanceof BackPressedHandler.HostActivity) {
            ((BackPressedHandler.HostActivity) getContext()).getBackPressedObserver().registerHandler(mFunctionsKeyboardHelper);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mOnRunFinishedReceiver);
        if (getContext() instanceof BackPressedHandler.HostActivity) {
            ((BackPressedHandler.HostActivity) getContext()).getBackPressedObserver().unregisterHandler(mFunctionsKeyboardHelper);
        }
    }

    public Uri getUri() {
        return mUri;
    }

    public Observable<String> handleIntent(Intent intent) {
        mName = intent.getStringExtra(EXTRA_NAME);
        return handleText(intent)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(str -> {
                    mReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false);
                    boolean saveEnabled = intent.getBooleanExtra(EXTRA_SAVE_ENABLED, true);
                    if (mReadOnly || !saveEnabled) {
                        findViewById(R.id.save).setVisibility(View.GONE);
                    }
                    if (!intent.getBooleanExtra(EXTRA_RUN_ENABLED, true)) {
                        findViewById(R.id.run).setVisibility(GONE);
                    }
                    if (mReadOnly) {
                        binding.editor.setReadOnly(true);
                    }
                });
    }

    public void setRestoredText(String text) {
        mRestoredText = text;
        binding.editor.setText(text);
    }

    private Observable<String> handleText(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        if (content != null) {
            setInitialText(content);
            return Observable.just(content);
        } else {
            if (path == null) {
                if (intent.getData() == null) {
                    return Observable.error(new IllegalArgumentException("path and content is empty"));
                } else {
                    mUri = intent.getData();
                }
            } else {
                mUri = Uri.fromFile(new File(path));
            }
            if (mName == null) {
                mName = PFiles.getNameWithoutExtension(mUri.getPath());
            }
            return loadUri(mUri);
        }
    }


    @SuppressLint("CheckResult")
    private Observable<String> loadUri(final Uri uri) {
        binding.editor.setProgress(true);
        return Observable.fromCallable(() -> {
                    // 对于 file:// URI，使用 FileProviderFactory 读取文件
                    // 这支持 SAF 模式和完全访问模式
                    // 传入路径参数，让工厂根据路径选择合适的 Provider
                    if ("file".equals(uri.getScheme())) {
                        return FileProviderFactory.getProvider(uri.getPath()).read(uri.getPath());
                    }
                    return PFiles.read(getContext().getContentResolver().openInputStream(uri));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(s -> {
                    setInitialText(s);
                    binding.editor.setProgress(false);
                });
    }

    private void setInitialText(String text) {
        if (mRestoredText != null) {
            binding.editor.setText(mRestoredText);
            mRestoredText = null;
            return;
        }
        binding.editor.setInitialText(text);
    }


    private void setMenuItemStatus(int id, boolean enabled) {
        mMenuItemStatus.put(id, enabled);
        ToolbarFragment fragment = (ToolbarFragment) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.toolbar_menu);
        if (fragment == null) {
            mNormalToolbar.setMenuItemStatus(id, enabled);
        } else {
            fragment.setMenuItemStatus(id, enabled);
        }
    }

    public boolean getMenuItemStatus(int id, boolean defValue) {
        return mMenuItemStatus.get(id, defValue);
    }

    private void initNormalToolbar() {
        mNormalToolbar.setOnMenuItemClickListener(this);
        mNormalToolbar.setOnMenuItemLongClickListener(id -> {
            if (id == R.id.run) {
                debug();
                return true;
            }
            return false;
        });
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.toolbar_menu);
        if (fragment == null) {
            showNormalToolbar();
        }
    }

    private void setUpFunctionsKeyboard() {
        mFunctionsKeyboardHelper = FunctionsKeyboardHelper.with((Activity) getContext())
                .setContent(binding.editor)
                .setFunctionsTrigger(binding.functions)
                .setFunctionsView(binding.functionsKeyboard)
                .setEditView(binding.editor.getCodeEditText())
                .build();
        binding.functionsKeyboard.setClickCallback(this);
    }

    private void setUpInputMethodEnhancedBar() {
        binding.symbolBar.setCodeCompletions(Symbol.getSymbols());
        binding.codeCompletionBar.setOnHintClickListener(this);
        binding.symbolBar.setOnHintClickListener(this);
        mAutoCompletion = new AutoCompletion(getContext(), binding.editor.getCodeEditText());
        mAutoCompletion.setAutoCompleteCallback(binding.codeCompletionBar::setCodeCompletions);
    }


    private void setUpEditor() {
        binding.editor.getCodeEditText().addTextChangedListener(new SimpleTextWatcher(s -> {
            setMenuItemStatus(R.id.save, binding.editor.isTextChanged());
            setMenuItemStatus(R.id.undo, binding.editor.canUndo());
            setMenuItemStatus(R.id.redo, binding.editor.canRedo());
        }));
        binding.editor.addCursorChangeCallback(this::autoComplete);
        binding.editor.getCodeEditText().setTextSize(Pref.getEditorTextSize((int) ViewUtils.pxToSp(getContext(), binding.editor.getCodeEditText().getTextSize())));
    }

    private void autoComplete(String line, int cursor) {
        mAutoCompletion.onCursorChange(line, cursor);
    }

    public DebugBar getDebugBar() {
        return binding.debugBar;
    }

    public void setTheme(Theme theme) {
        mEditorTheme = theme;
        binding.editor.setTheme(theme);
        binding.inputMethodEnhanceBar.setBackgroundColor(theme.getImeBarBackgroundColor());
        int textColor = theme.getImeBarForegroundColor();
        binding.codeCompletionBar.setTextColor(textColor);
        binding.symbolBar.setTextColor(textColor);
        binding.functions.setColorFilter(textColor);
        invalidate();
    }

    public boolean onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (binding.docs.getWebView().canGoBack()) {
                binding.docs.getWebView().goBack();
            } else {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onToolbarMenuItemClick(int id) {
        switch (id) {
            case R.id.run:
                runAndSaveFileIfNeeded();
                break;
            case R.id.save:
                saveFile();
                break;
            case R.id.undo:
                undo();
                break;
            case R.id.redo:
                redo();
                break;
            case R.id.replace:
                replace();
                break;
            case R.id.find_next:
                findNext();
                break;
            case R.id.find_prev:
                findPrev();
                break;
            case R.id.cancel_search:
                cancelSearch();
                break;
        }
    }

    @SuppressLint("CheckResult")
    public void runAndSaveFileIfNeeded() {
        save().observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> run(true), Observers.toastMessage());
    }

    public ScriptExecution run(boolean showMessage) {
        if (showMessage) {
            Snackbar.make(this, R.string.text_start_running, Snackbar.LENGTH_SHORT).show();
        }
        // TODO: 2018/10/24
        ScriptExecution execution = Scripts.INSTANCE.runWithBroadcastSender(new File(mUri.getPath()));
        if (execution == null) {
            return null;
        }
        mScriptExecutionId = execution.getId();
        setMenuItemStatus(R.id.run, false);
        return execution;
    }


    public void undo() {
        binding.editor.undo();
    }

    public void redo() {
        binding.editor.redo();
    }

    public Observable<String> save() {
        String path = mUri.getPath();
        // 使用 FileProviderFactory 创建备份，支持 SAF 模式
        IFileProvider provider = FileProviderFactory.getProvider(path);
        provider.copy(path, path + ".bak");
        return Observable.just(binding.editor.getText())
                .observeOn(Schedulers.io())
                .doOnNext(s -> {
                    // 对于 file:// URI，使用 FileProviderFactory 写入文件
                    // 这支持 SAF 模式和完全访问模式
                    // 传入路径参数，让工厂根据路径选择合适的 Provider
                    if ("file".equals(mUri.getScheme())) {
                        FileProviderFactory.getProvider(path).write(path, s);
                    } else {
                        PFiles.write(getContext().getContentResolver().openOutputStream(mUri), s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(s -> {
                    binding.editor.markTextAsSaved();
                    setMenuItemStatus(R.id.save, false);
                });
    }

    public void forceStop() {
        doWithCurrentEngine(ScriptEngine::forceStop);
    }

    private void doWithCurrentEngine(Callback<ScriptEngine> callback) {
        ScriptExecution execution = AutoJs.getInstance().getScriptEngineService().getScriptExecution(mScriptExecutionId);
        if (execution != null) {
            ScriptEngine engine = execution.getEngine();
            if (engine != null) {
                callback.call(engine);
            }
        }
    }

    @SuppressLint("CheckResult")
    public void saveFile() {
        save()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Observers.emptyConsumer(), e -> {
                    e.printStackTrace();
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    void findNext() {
        binding.editor.findNext();
    }

    void findPrev() {
        binding.editor.findPrev();
    }

    void cancelSearch() {
        showNormalToolbar();
    }

    private void showNormalToolbar() {
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.toolbar_menu, mNormalToolbar)
                .commitAllowingStateLoss();
    }

    FragmentActivity getActivity() {
        Context context = getContext();
        while (!(context instanceof Activity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return (FragmentActivity) context;
    }

    void replace() {
        binding.editor.replaceSelection();
    }

    public String getName() {
        return mName;
    }

    public boolean isTextChanged() {
        return binding.editor.isTextChanged();
    }

    public void showConsole() {
        doWithCurrentEngine(engine -> ((JavaScriptEngine) engine).getRuntime().console.show());
    }

    /**
     * Set callback for Material3 LogSheet integration
     * This should be called from EditActivity to enable log panel functionality
     */
    public void setLogPanelCallback(LogPanelCallback callback) {
        mLogPanelCallback = callback;
    }

    /**
     * Show the Material3 log panel (LogSheet)
     * This triggers the callback to show the ModalBottomSheet
     */
    public void showLogPanel() {
        if (mLogPanelCallback != null) {
            mLogPanelCallback.onShowLogPanel();
        }
    }

    public void openByOtherApps() {
        if (mUri != null) {
            Scripts.INSTANCE.openByOtherApps(mUri);
        }
    }

    public void beautifyCode() {
        binding.editor.beautifyCode();
    }

    public void selectEditorTheme() {
        binding.editor.setProgress(true);
        Themes.getAllThemes(getContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(themes -> {
                    binding.editor.setProgress(false);
                    selectEditorTheme(themes);
                });
    }

    public void selectTextSize() {
        new TextSizeSettingDialogBuilder(getContext())
                .initialValue((int) ViewUtils.pxToSp(getContext(), binding.editor.getCodeEditText().getTextSize()))
                .callback(this::setTextSize)
                .show();
    }

    public void setTextSize(int value) {
        Pref.setEditorTextSize(value);
        binding.editor.getCodeEditText().setTextSize(value);
    }

    private void selectEditorTheme(List<Theme> themes) {
        int i = themes.indexOf(mEditorTheme);
        if (i < 0) {
            i = 0;
        }
        new MaterialDialog.Builder(getContext())
                .title(R.string.text_editor_theme)
                .items(themes)
                .itemsCallbackSingleChoice(i, (dialog, itemView, which, text) -> {
                    setTheme(themes.get(which));
                    Themes.setCurrent(themes.get(which).getName());
                    return true;
                })
                .show();
    }

    public CodeEditor getEditor() {
        return binding.editor;
    }

    public void find(String keywords, boolean usingRegex) throws CodeEditor.CheckedPatternSyntaxException {
        binding.editor.find(keywords, usingRegex);
        showSearchToolbar(false);
    }

    private void showSearchToolbar(boolean showReplaceItem) {
        SearchToolbarFragment searchToolbarFragment = new SearchToolbarFragment();
        Bundle args = new Bundle();
        args.putBoolean(SearchToolbarFragment.ARGUMENT_SHOW_REPLACE_ITEM, showReplaceItem);
        searchToolbarFragment.setArguments(args);
        searchToolbarFragment.setOnMenuItemClickListener(this);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.toolbar_menu, searchToolbarFragment)
                .commit();
    }

    public void replace(String keywords, String replacement, boolean usingRegex) throws CodeEditor.CheckedPatternSyntaxException {
        binding.editor.replace(keywords, replacement, usingRegex);
        showSearchToolbar(true);
    }

    public void replaceAll(String keywords, String replacement, boolean usingRegex) throws CodeEditor.CheckedPatternSyntaxException {
        binding.editor.replaceAll(keywords, replacement, usingRegex);
    }


    public void debug() {
        DebugToolbarFragment debugToolbarFragment = new DebugToolbarFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.toolbar_menu, debugToolbarFragment)
                .commit();
        binding.debugBar.setVisibility(VISIBLE);
        binding.inputMethodEnhanceBar.setVisibility(GONE);
        mDebugging = true;
    }

    public void exitDebugging() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.toolbar_menu);
        if (fragment instanceof DebugToolbarFragment) {
            ((DebugToolbarFragment) fragment).detachDebugger();
        }
        showNormalToolbar();
        binding.editor.setDebuggingLine(-1);
        binding.debugBar.setVisibility(GONE);
        binding.inputMethodEnhanceBar.setVisibility(VISIBLE);
        mDebugging = false;
    }

    private void showErrorMessage(String msg) {
        Snackbar.make(EditorView.this, getResources().getString(R.string.text_error) + ": " + msg, Snackbar.LENGTH_LONG)
                .setAction(R.string.text_detail, v -> getContext().startActivity(new Intent(getContext(), LogActivity.class)))
                .show();
    }

    @Override
    public void onHintClick(CodeCompletions completions, int pos) {
        CodeCompletion completion = completions.get(pos);
        binding.editor.insert(completion.getInsertText());
    }

    @Override
    public void onHintLongClick(CodeCompletions completions, int pos) {
        CodeCompletion completion = completions.get(pos);
        if (Objects.equals(completion.getHint(), "/")) {
            getEditor().toggleComment();
            return;
        }
        if (completion.getUrl() == null)
            return;
        showManual(completion.getUrl(), completion.getHint());
    }

    private void showManual(String url, String title) {
        String absUrl = Pref.getDocumentationUrl() + url;
        new ManualDialog(getContext())
                .title(title)
                .url(absUrl)
                .pinToLeft(v -> {
                    binding.docs.getWebView().loadUrl(absUrl);
                    binding.drawerLayout.openDrawer(GravityCompat.START);
                })
                .show();
    }

    @Override
    public void onModuleLongClick(Module module) {
        showManual(module.getUrl(), module.getName());
    }

    @Override
    public void onPropertyClick(Module m, Property property) {
        String p = property.getKey();
        if (!property.isVariable()) {
            p = p + "()";
        }
        if (property.isGlobal()) {
            binding.editor.insert(p);
        } else {
            binding.editor.insert(m.getName() + "." + p);
        }
        if (!property.isVariable()) {
            binding.editor.moveCursor(-1);
        }
        mFunctionsKeyboardHelper.hideFunctionsLayout(true);
    }

    @Override
    public void onPropertyLongClick(Module m, Property property) {
        if (TextUtils.isEmpty(property.getUrl())) {
            showManual(m.getUrl(), property.getKey());
        } else {
            showManual(property.getUrl(), property.getKey());
        }
    }

    public int getScriptExecutionId() {
        return mScriptExecutionId;
    }

    @Nullable
    public ScriptExecution getScriptExecution() {
        return AutoJs.getInstance().getScriptEngineService().getScriptExecution(mScriptExecutionId);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        Parcelable superData = super.onSaveInstanceState();
        bundle.putParcelable("super_data", superData);
        bundle.putInt("script_execution_id", mScriptExecutionId);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        Parcelable superData = bundle.getParcelable("super_data");
        mScriptExecutionId = bundle.getInt("script_execution_id", ScriptExecution.NO_ID);
        super.onRestoreInstanceState(superData);
        setMenuItemStatus(R.id.run, mScriptExecutionId == ScriptExecution.NO_ID);
    }

    public void destroy() {
        binding.editor.destroy();
        mAutoCompletion.shutdown();
    }
}