package org.autojs.autojs.ui.log;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.stardust.autojs.core.console.ConsoleView;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.BottomSheetLogBinding;

/**
 * Bottom sheet dialog for displaying script logs in the editor
 * Provides quick access to log output without leaving the editor
 * Clickable stack frames (file:line) are highlighted in blue
 */
public class LogBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SCRIPT_NAME = "script_name";
    private static final String ARG_SCRIPT_PATH = "script_path";

    private BottomSheetLogBinding binding;
    private String mScriptName;
    private String mScriptPath;
    private OnStackFrameClickListener mStackFrameClickListener;

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
            
            // Enable clickable stack frame links (only in bottom sheet, not in LogActivity)
            binding.console.setEnableStackFrameLinks(true);
            
            // Set up clickable stack frame listener
            binding.console.setOnStackFrameClickListener((fileName, lineNumber, columnNumber) -> {
                if (mStackFrameClickListener != null) {
                    mStackFrameClickListener.onStackFrameClick(fileName, lineNumber, columnNumber);
                    dismiss();
                }
            });
        }

        // Clear button
        binding.btnClear.setOnClickListener(v -> {
            AutoJs instance = AutoJs.getInstance();
            if (instance != null) {
                instance.getGlobalConsole().clear();
            }
        });

        // Open full log activity button
        binding.btnOpenFull.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LogActivity.class));
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
