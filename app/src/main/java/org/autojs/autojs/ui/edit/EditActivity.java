package org.autojs.autojs.ui.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback;
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity;
import com.stardust.autojs.core.permission.RequestPermissionCallbacks;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.pio.PFiles;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityEditBinding;
import org.autojs.autojs.storage.file.TmpScriptFiles;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.main.MainActivity;

import java.io.File;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static org.autojs.autojs.ui.edit.EditorView.EXTRA_CONTENT;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_NAME;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_PATH;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_READ_ONLY;

/**
 * Created by Stardust on 2017/1/29.
 */
public class EditActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost, PermissionRequestProxyActivity {

    private OnActivityResultDelegate.Mediator mMediator = new OnActivityResultDelegate.Mediator();
    private static final String LOG_TAG = "EditActivity";
    
    // Extra constants for jumping to specific line
    public static final String EXTRA_JUMP_LINE = "jump_line";
    public static final String EXTRA_JUMP_COLUMN = "jump_column";

    private ActivityEditBinding binding;
    private EditorView mEditorView;
    private EditorMenu mEditorMenu;
    private RequestPermissionCallbacks mRequestPermissionCallbacks = new RequestPermissionCallbacks();
    private boolean mNewTask;

    public static void editFile(Context context, String path, boolean newTask) {
        editFile(context, null, path, newTask);
    }

    public static void editFile(Context context, Uri uri, boolean newTask) {
        context.startActivity(newIntent(context, newTask)
                .setData(uri));
    }

    public static void editFile(Context context, String name, String path, boolean newTask) {
        context.startActivity(newIntent(context, newTask)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_NAME, name));
    }

    public static void viewContent(Context context, String name, String content, boolean newTask) {
        context.startActivity(newIntent(context, newTask)
                .putExtra(EXTRA_CONTENT, content)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_READ_ONLY, true));
    }

    private static Intent newIntent(Context context, boolean newTask) {
        Intent intent = new Intent(context, EditActivity.class);
        if (newTask || !(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        mEditorView = binding.editorView;
        mNewTask = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
        
        setUpViews();
        setUpLogSheet();
    }

    @SuppressLint("CheckResult")
    private void setUpViews() {
        Intent intent = getIntent();
        mEditorView.handleIntent(intent)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    v -> handleJumpToLine(intent),
                    ex -> onLoadFileError(ex.getMessage())
                );
        mEditorMenu = new EditorMenu(mEditorView);
        setUpToolbar();
    }
    
    /**
     * Handle jumping to specific line if specified in Intent extras
     */
    private void handleJumpToLine(Intent intent) {
        if (intent == null) return;
        
        int jumpLine = intent.getIntExtra(EXTRA_JUMP_LINE, -1);
        int jumpColumn = intent.getIntExtra(EXTRA_JUMP_COLUMN, 0);
        
        if (jumpLine >= 0) {
            // Delay jump to ensure editor is fully loaded
            mEditorView.postDelayed(() -> {
                mEditorView.getEditor().jumpTo(jumpLine, jumpColumn);
            }, 100);
            
            // Clear extras to prevent re-jumping on configuration change
            intent.removeExtra(EXTRA_JUMP_LINE);
            intent.removeExtra(EXTRA_JUMP_COLUMN);
        }
    }
    
    /**
     * Set up log panel integration using BottomSheetDialogFragment
     */
    private void setUpLogSheet() {
        mEditorView.setLogPanelCallback(() -> {
            // Get current script name and path
            String scriptName = mEditorView.getName();
            String scriptPath = mEditorView.getUri() != null ? mEditorView.getUri().getPath() : null;
            
            // Show log bottom sheet
            org.autojs.autojs.ui.log.LogBottomSheet bottomSheet = 
                org.autojs.autojs.ui.log.LogBottomSheet.newInstance(scriptName, scriptPath);
            
            // Set up stack frame click listener for jumping to source lines
            bottomSheet.setOnStackFrameClickListener((fileName, lineNumber, columnNumber) -> {
                // Get current file path
                String currentPath = mEditorView.getUri() != null ? mEditorView.getUri().getPath() : null;
                
                // Check if the clicked file matches the current file
                // fileName could be a full path or just a filename
                boolean isCurrentFile = false;
                if (currentPath != null) {
                    if (fileName.equals(currentPath)) {
                        isCurrentFile = true;
                    } else if (currentPath.endsWith(fileName) || currentPath.endsWith("/" + fileName)) {
                        isCurrentFile = true;
                    } else {
                        // Try to resolve relative path
                        File clickedFile = new File(fileName);
                        if (!clickedFile.isAbsolute()) {
                            // Relative path - resolve from current file's directory
                            File currentDir = new File(currentPath).getParentFile();
                            if (currentDir != null) {
                                File resolvedFile = new File(currentDir, fileName);
                                if (resolvedFile.exists()) {
                                    fileName = resolvedFile.getAbsolutePath();
                                    isCurrentFile = resolvedFile.getAbsolutePath().equals(currentPath);
                                }
                            }
                        }
                    }
                }
                
                if (isCurrentFile) {
                    // Same file - just jump to line
                    mEditorView.getEditor().jumpTo(lineNumber, columnNumber);
                } else {
                    // Different file - need to open it
                    File targetFile = new File(fileName);
                    if (targetFile.exists()) {
                        // Check if current file has unsaved changes
                        if (mEditorView.isTextChanged()) {
                            new ThemeColorMaterialDialogBuilder(this)
                                .title(R.string.text_alert)
                                .content(R.string.edit_exit_without_save_warn)
                                .positiveText(R.string.text_cancel)
                                .negativeText(R.string.text_save_and_exit)
                                .neutralText(R.string.text_exit_directly)
                                .onNegative((dialog, which) -> {
                                    mEditorView.saveFile();
                                    openFileAndJump(targetFile.getAbsolutePath(), lineNumber, columnNumber);
                                })
                                .onNeutral((dialog, which) -> {
                                    openFileAndJump(targetFile.getAbsolutePath(), lineNumber, columnNumber);
                                })
                                .show();
                        } else {
                            openFileAndJump(targetFile.getAbsolutePath(), lineNumber, columnNumber);
                        }
                    } else {
                        // File not found - just show toast
                        Toast.makeText(this, "文件不存在: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
            bottomSheet.show(getSupportFragmentManager(), "LogBottomSheet");
        });
    }
    
    /**
     * Open a file and jump to specified line
     */
    private void openFileAndJump(String path, int lineNumber, int columnNumber) {
        // Create new intent with the file path
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_JUMP_LINE, lineNumber);
        intent.putExtra(EXTRA_JUMP_COLUMN, columnNumber);
        
        // Finish current activity and start new one
        finish();
        startActivity(intent);
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return super.onWindowStartingActionMode(callback);
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
        return super.onWindowStartingActionMode(callback, type);
    }

    private void onLoadFileError(String message) {
        new ThemeColorMaterialDialogBuilder(this)
                .title(getString(R.string.text_cannot_read_file))
                .content(message)
                .positiveText(R.string.text_exit)
                .cancelable(false)
                .onPositive((dialog, which) -> finish())
                .show();
    }

    private void setUpToolbar() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, mEditorView.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mEditorMenu.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onPrepareOptionsMenu: " + menu);
        boolean isScriptRunning = mEditorView.getScriptExecutionId() != ScriptExecution.NO_ID;
        MenuItem forceStopItem = menu.findItem(R.id.action_force_stop);
        forceStopItem.setEnabled(isScriptRunning);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        Log.d(LOG_TAG, "onActionModeStarted: " + mode);
        Menu menu = mode.getMenu();
        MenuItem item = menu.getItem(menu.size() - 1);
        // 以下两项在MIUI设备上存在崩溃bug，暂时注释掉，相关功能可通过编辑菜单使用
//        menu.add(item.getGroupId(), R.id.action_delete_line, 10000, R.string.text_delete_line);
//        menu.add(item.getGroupId(), R.id.action_copy_line, 20000, R.string.text_copy_line);
        super.onActionModeStarted(mode);
    }

    @Override
    public void onSupportActionModeStarted(@NonNull androidx.appcompat.view.ActionMode mode) {
        Log.d(LOG_TAG, "onSupportActionModeStarted: mode = " + mode);
        super.onSupportActionModeStarted(mode);
    }

    @Nullable
    @Override
    public androidx.appcompat.view.ActionMode onWindowStartingSupportActionMode(@NonNull androidx.appcompat.view.ActionMode.Callback callback) {
        Log.d(LOG_TAG, "onWindowStartingSupportActionMode: callback = " + callback);
        return super.onWindowStartingSupportActionMode(callback);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        Log.d(LOG_TAG, "startActionMode: callback = " + callback + ", type = " + type);
        return super.startActionMode(callback, type);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        Log.d(LOG_TAG, "startActionMode: callback = " + callback);
        return super.startActionMode(callback);
    }

    @Override
    public void onBackPressed() {
        if (!mEditorView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        if (mEditorView.isTextChanged()) {
            showExitConfirmDialog();
            return;
        }
        finishAndRemoveFromRecents();
    }

    private void finishAndRemoveFromRecents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            super.finish();
        }
        if (mNewTask) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private void showExitConfirmDialog() {
        new ThemeColorMaterialDialogBuilder(this)
                .title(R.string.text_alert)
                .content(R.string.edit_exit_without_save_warn)
                .positiveText(R.string.text_cancel)
                .negativeText(R.string.text_save_and_exit)
                .neutralText(R.string.text_exit_directly)
                .onNegative((dialog, which) -> {
                    mEditorView.saveFile();
                    finishAndRemoveFromRecents();
                })
                .onNeutral((dialog, which) -> finishAndRemoveFromRecents())
                .show();
    }

    @Override
    protected void onDestroy() {
        mEditorView.destroy();
        super.onDestroy();
    }

    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mMediator;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMediator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!mEditorView.isTextChanged()) {
            return;
        }
        String text = mEditorView.getEditor().getText();
        if (text.length() < 256 * 1024) {
            outState.putString("text", text);
        } else {
            File tmp = saveToTmpFile(text);
            if (tmp != null) {
                outState.putString("path", tmp.getPath());
            }

        }
    }

    private File saveToTmpFile(String text) {
        try {
            File tmp = TmpScriptFiles.create(this);
            Observable.just(text)
                    .observeOn(Schedulers.io())
                    .subscribe(t -> PFiles.write(tmp, t));
            return tmp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String text = savedInstanceState.getString("text");
        if (text != null) {
            mEditorView.setRestoredText(text);
            return;
        }
        String path = savedInstanceState.getString("path");
        if (path != null) {
            Observable.just(path)
                    .observeOn(Schedulers.io())
                    .map(PFiles::read)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(t -> mEditorView.getEditor().setText(t), Throwable::printStackTrace);
        }
    }

    @Override
    public void addRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        mRequestPermissionCallbacks.addCallback(callback);
    }

    @Override
    public boolean removeRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        return mRequestPermissionCallbacks.removeCallback(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestPermissionCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}