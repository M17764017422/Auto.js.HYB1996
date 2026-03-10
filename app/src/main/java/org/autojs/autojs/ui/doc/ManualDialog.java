package org.autojs.autojs.ui.doc;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FloatingManualDialogBinding;
import org.autojs.autojs.ui.widget.EWebView;

/**
 * Created by Stardust on 2017/10/24.
 */

public class ManualDialog {

    private FloatingManualDialogBinding binding;
    Dialog mDialog;
    private Context mContext;

    public ManualDialog(Context context) {
        mContext = context;
        binding = FloatingManualDialogBinding.inflate(LayoutInflater.from(context));
        mDialog = new MaterialDialog.Builder(context)
                .customView(binding.getRoot(), false)
                .build();
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        binding.close.setOnClickListener(v -> mDialog.dismiss());
        binding.fullscreen.setOnClickListener(v -> viewInNewActivity());
    }


    public ManualDialog title(String title) {
        binding.title.setText(title);
        return this;
    }

    public ManualDialog url(String url) {
        binding.ewebView.getWebView().loadUrl(url);
        return this;
    }

    public ManualDialog pinToLeft(View.OnClickListener listener) {
        binding.pinToLeft.setOnClickListener(v -> {
            mDialog.dismiss();
            listener.onClick(v);
        });
        return this;
    }

    public ManualDialog show() {
        mDialog.show();
        return this;
    }

    private void viewInNewActivity() {
        mDialog.dismiss();
        Intent intent = new Intent(mContext, DocumentationActivity.class);
        intent.putExtra(DocumentationActivity.EXTRA_URL, binding.ewebView.getWebView().getUrl());
        mContext.startActivity(intent);
    }

}