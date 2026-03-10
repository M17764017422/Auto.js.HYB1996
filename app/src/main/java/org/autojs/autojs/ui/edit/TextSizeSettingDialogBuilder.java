package org.autojs.autojs.ui.edit;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.SeekBar;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.DialogTextSizeSettingBinding;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;

/**
 * Created by Stardust on 2018/2/24.
 */

public class TextSizeSettingDialogBuilder extends ThemeColorMaterialDialogBuilder implements SeekBar.OnSeekBarChangeListener {


    public interface PositiveCallback {

        void onPositive(int value);
    }

    private static final int MIN = 8;

    private DialogTextSizeSettingBinding binding;
    private int mTextSize;
    private MaterialDialog mMaterialDialog;

    public TextSizeSettingDialogBuilder(@NonNull Context context) {
        super(context);
        binding = DialogTextSizeSettingBinding.inflate(LayoutInflater.from(context));
        customView(binding.getRoot(), false);
        title(R.string.text_text_size);
        positiveText(R.string.ok);
        negativeText(R.string.cancel);
        binding.seekbar.setOnSeekBarChangeListener(this);
    }

    private void setTextSize(int textSize) {
        mTextSize = textSize;
        String title = getContext().getString(R.string.text_size_current_value, textSize);
        if (mMaterialDialog != null) {
            mMaterialDialog.setTitle(title);
        } else {
            title(title);
        }
        binding.previewText.setTextSize(textSize);
    }

    public TextSizeSettingDialogBuilder initialValue(int value) {
        binding.seekbar.setProgress(value - MIN);
        return this;
    }

    public TextSizeSettingDialogBuilder callback(PositiveCallback callback) {
        onPositive((dialog, which) -> callback.onPositive(mTextSize));
        return this;
    }

    @Override
    public MaterialDialog build() {
        mMaterialDialog = super.build();
        return mMaterialDialog;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setTextSize(progress + MIN);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}