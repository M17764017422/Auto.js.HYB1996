package org.autojs.autojs.ui.main.task;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.FragmentTaskManagerBinding;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.widget.SimpleAdapterDataObserver;

/**
 * Created by Stardust on 2017/3/24.
 */
public class TaskManagerFragment extends ViewPagerFragment {

    private FragmentTaskManagerBinding binding;
    private TaskListRecyclerView mTaskListRecyclerView;
    private View mNoRunningScriptNotice;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public TaskManagerFragment() {
        super(45);
        setArguments(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTaskListRecyclerView = binding.taskList;
        mNoRunningScriptNotice = binding.noticeNoRunningScript;
        mSwipeRefreshLayout = binding.swipeRefreshLayout;
        setUpViews();
    }

    private void setUpViews() {
        init();
        final boolean noRunningScript = mTaskListRecyclerView.getAdapter().getItemCount() == 0;
        mNoRunningScriptNotice.setVisibility(noRunningScript ? View.VISIBLE : View.GONE);
    }

    private void init() {
        mTaskListRecyclerView.getAdapter().registerAdapterDataObserver(new SimpleAdapterDataObserver() {

            @Override
            public void onSomethingChanged() {
                final boolean noRunningScript = mTaskListRecyclerView.getAdapter().getItemCount() == 0;
                mTaskListRecyclerView.postDelayed(() -> {
                    if (mNoRunningScriptNotice == null)
                        return;
                    mNoRunningScriptNotice.setVisibility(noRunningScript ? View.VISIBLE : View.GONE);
                }, 150);
            }

        });
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mTaskListRecyclerView.refresh();
            mTaskListRecyclerView.postDelayed(() -> {
                if (mSwipeRefreshLayout != null)
                    mSwipeRefreshLayout.setRefreshing(false);
            }, 800);
        });
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {
        AutoJs.getInstance().getScriptEngineService().stopAll();
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        return false;
    }
}