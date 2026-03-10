package org.autojs.autojs.ui.user;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.stardust.app.OnActivityResultDelegate;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityWebBinding;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.widget.EWebView;

/**
 * Created by Stardust on 2017/10/26.
 */
public class WebActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost {

    public static final String EXTRA_URL = "url";

    private OnActivityResultDelegate.Mediator mMediator = new OnActivityResultDelegate.Mediator();
    private ActivityWebBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
    }

    private void setupViews() {
        setToolbarAsBack(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        binding.ewebView.getWebView().loadUrl(getIntent().getStringExtra(EXTRA_URL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMediator.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mMediator;
    }
}