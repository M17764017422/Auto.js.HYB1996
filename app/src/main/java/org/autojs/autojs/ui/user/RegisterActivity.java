package org.autojs.autojs.ui.user;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityRegisterBinding;
import org.autojs.autojs.network.NodeBB;
import org.autojs.autojs.network.UserService;
import org.autojs.autojs.ui.BaseActivity;
import com.stardust.theme.ThemeColorManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Stardust on 2017/10/26.
 */
public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setUpViews();
    }

    private void setUpViews() {
        setToolbarAsBack(getString(R.string.text_register));
        ThemeColorManager.addViewBackground(binding.register);
        
        binding.register.setOnClickListener(v -> register());
    }

    void register() {
        String email = binding.email.getText().toString();
        String userName = binding.username.getText().toString();
        String password = binding.password.getText().toString();
        if (!validateInput(email, userName, password)) {
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .progress(true, 0)
                .content(R.string.text_registering)
                .cancelable(false)
                .show();
        UserService.getInstance().register(email, userName, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            dialog.dismiss();
                            onRegisterResponse(response.string());
                        }
                        , error -> {
                            dialog.dismiss();
                            binding.password.setError(NodeBB.getErrorMessage(error, RegisterActivity.this, R.string.text_register_fail));
                        });

    }

    private void onRegisterResponse(String res) {
        Toast.makeText(this, R.string.text_register_succeed, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validateInput(String email, String userName, String password) {
        if (email.isEmpty()) {
            binding.email.setError(getString(R.string.text_email_cannot_be_empty));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError(getString(R.string.text_email_format_error));
            return false;
        }
        if (userName.isEmpty()) {
            binding.username.setError(getString(R.string.text_username_cannot_be_empty));
            return false;
        }
        if (password.isEmpty()) {
            binding.username.setError(getString(R.string.text_password_cannot_be_empty));
            return false;
        }
        if (password.length() < 6) {
            binding.password.setError(getString(R.string.nodebb_error_change_password_error_length));
            return false;
        }
        return true;
    }
}