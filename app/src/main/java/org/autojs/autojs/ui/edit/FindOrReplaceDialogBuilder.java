package org.autojs.autojs.ui.edit;

import android.content.Context;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.DialogFindOrReplaceBinding;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.edit.editor.CodeEditor;

/**
 * Created by Stardust on 2017/9/28.
 */

public class FindOrReplaceDialogBuilder extends ThemeColorMaterialDialogBuilder {

    private static final String KEY_KEYWORDS = "...";

    private DialogFindOrReplaceBinding binding;
    private EditorView mEditorView;

    public FindOrReplaceDialogBuilder(@NonNull Context context, EditorView editorView) {
        super(context);
        mEditorView = editorView;
        setupViews();
        restoreState();
        autoDismiss(false);
        onNegative((dialog, which) -> dialog.dismiss());
        onPositive((dialog, which) -> {
            storeState();
            findOrReplace(dialog);
        });
    }

    private void setupViews() {
        binding = DialogFindOrReplaceBinding.inflate(LayoutInflater.from(context));
        customView(binding.getRoot(), true);
        positiveText(R.string.ok);
        negativeText(R.string.cancel);
        title(R.string.text_find_or_replace);
        
        binding.checkboxReplaceAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !binding.checkboxReplace.isChecked()) {
                    binding.checkboxReplace.setChecked(true);
                }
            }
        });
        
        binding.replacement.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.checkboxReplace.setChecked(true);
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }


    private void storeState() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(KEY_KEYWORDS, binding.keywords.getText().toString())
                .apply();
    }


    private void restoreState() {
        binding.keywords.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(KEY_KEYWORDS, ""));
    }

    private void findOrReplace(MaterialDialog dialog) {
        String keywords = binding.keywords.getText().toString();
        if (keywords.isEmpty()) {
            return;
        }
        try {
            boolean usingRegex = binding.checkboxRegex.isChecked();
            if (!binding.checkboxReplace.isChecked()) {
                mEditorView.find(keywords, usingRegex);
            } else {
                String replacement = binding.replacement.getText().toString();
                if (binding.checkboxReplaceAll.isChecked()) {
                    mEditorView.replaceAll(keywords, replacement, usingRegex);
                } else {
                    mEditorView.replace(keywords, replacement, usingRegex);
                }
            }
            dialog.dismiss();
        } catch (CodeEditor.CheckedPatternSyntaxException e) {
            e.printStackTrace();
            binding.keywords.setError(getContext().getString(R.string.error_pattern_syntax));
        }

    }

    public FindOrReplaceDialogBuilder setQueryIfNotEmpty(String s) {
        if (!TextUtils.isEmpty(s))
            binding.keywords.setText(s);
        return this;
    }
}