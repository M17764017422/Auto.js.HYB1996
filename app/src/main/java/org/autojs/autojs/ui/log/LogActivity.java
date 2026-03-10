package org.autojs.autojs.ui.log;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;

import com.stardust.autojs.core.console.ConsoleView;
import com.stardust.autojs.core.console.ConsoleImpl;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ActivityLogBinding;
import org.autojs.autojs.ui.BaseActivity;

public class LogActivity extends BaseActivity {

    private ActivityLogBinding binding;
    private ConsoleImpl mConsoleImpl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyDayNightMode();
        setupViews();
    }

    private void setupViews() {
        setToolbarAsBack(getString(R.string.text_log));
        mConsoleImpl = AutoJs.getInstance().getGlobalConsole();
        binding.console.setConsole(mConsoleImpl);
        binding.console.findViewById(R.id.input_container).setVisibility(View.GONE);
        
        binding.fab.setOnClickListener(v -> clearConsole());
    }

    void clearConsole() {
        mConsoleImpl.clear();
    }
}