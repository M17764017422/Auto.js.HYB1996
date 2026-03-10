package org.autojs.autojs.ui.main.scripts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.stardust.app.GlobalAppContext;
import com.stardust.util.IntentUtil;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FragmentMyScriptListBinding;
import org.autojs.autojs.external.fileprovider.AppFileProvider;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.storage.FileProviderFactory;
import org.autojs.autojs.tool.SimpleObserver;
import org.autojs.autojs.ui.common.ScriptOperations;
import org.autojs.autojs.ui.explorer.ExplorerView;
import org.autojs.autojs.ui.main.FloatingActionMenu;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.project.ProjectConfigActivity;
import org.autojs.autojs.ui.project.ProjectConfigActivity;
import org.autojs.autojs.ui.viewmodel.ExplorerItemList;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Stardust on 2017/3/13.
 */
public class MyScriptListFragment extends ViewPagerFragment implements FloatingActionMenu.OnFloatingActionButtonClickListener {

    private static final String TAG = "MyScriptListFragment";

    public MyScriptListFragment() {
        super(0);
    }

    private FragmentMyScriptListBinding binding;
    private FloatingActionMenu mFloatingActionMenu;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyScriptListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViews();
    }

    private void setUpViews() {
        ExplorerItemList.SortConfig sortConfig = ExplorerItemList.SortConfig.from(PreferenceManager.getDefaultSharedPreferences(getContext()));
        binding.scriptFileList.setSortConfig(sortConfig);
        binding.scriptFileList.setExplorer(Explorers.workspace(), ExplorerDirPage.createRoot(FileProviderFactory.getProvider().getWorkingDirectory()));
        binding.scriptFileList.setOnItemClickListener((v, item) -> {
            if (item.isEditable()) {
                Scripts.INSTANCE.edit(getActivity(), item.toScriptFile());
            } else {
                IntentUtil.viewFile(GlobalAppContext.get(), item.getPath(), AppFileProvider.AUTHORITY);
            }
        });
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {
        initFloatingActionMenuIfNeeded(fab);
        if (mFloatingActionMenu.isExpanded()) {
            mFloatingActionMenu.collapse();
        } else {
            mFloatingActionMenu.expand();

        }
    }

    private void initFloatingActionMenuIfNeeded(final FloatingActionButton fab) {
        if (mFloatingActionMenu != null)
            return;
        mFloatingActionMenu = getActivity().findViewById(R.id.floating_action_menu);
        mFloatingActionMenu.getState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Boolean>() {
                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Boolean expanding) {
                        fab.animate()
                                .rotation(expanding ? 45 : 0)
                                .setDuration(300)
                                .start();
                    }
                });
        mFloatingActionMenu.setOnFloatingActionButtonClickListener(this);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mFloatingActionMenu != null && mFloatingActionMenu.isExpanded()) {
            mFloatingActionMenu.collapse();
            return true;
        }
        if (binding.scriptFileList.canGoBack()) {
            binding.scriptFileList.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void onPageHide() {
        super.onPageHide();
        if (mFloatingActionMenu != null && mFloatingActionMenu.isExpanded()) {
            mFloatingActionMenu.collapse();
        }
    }

    @Subscribe
    public void onQuerySummit(QueryEvent event) {
        if (!isShown()) {
            return;
        }
        if (event == QueryEvent.CLEAR) {
            binding.scriptFileList.setFilter(null);
            return;
        }
        String query = event.getQuery();
        binding.scriptFileList.setFilter((item -> item.getName().contains(query)));
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.scriptFileList.getSortConfig().saveInto(PreferenceManager.getDefaultSharedPreferences(getContext()));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mFloatingActionMenu != null)
            mFloatingActionMenu.setOnFloatingActionButtonClickListener(null);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(FloatingActionButton button, int pos) {
        if (binding.scriptFileList == null)
            return;
        switch (pos) {
            case 0:
                new ScriptOperations(getContext(), binding.scriptFileList, binding.scriptFileList.getCurrentPage())
                        .newDirectory();
                break;
            case 1:
                new ScriptOperations(getContext(), binding.scriptFileList, binding.scriptFileList.getCurrentPage())
                        .newFile();
                break;
            case 2:
                new ScriptOperations(getContext(), binding.scriptFileList, binding.scriptFileList.getCurrentPage())
                        .importFile();
                break;
            case 3:
                Intent intent = new Intent(getContext(), ProjectConfigActivity.class);
                intent.putExtra(ProjectConfigActivity.EXTRA_PARENT_DIRECTORY, binding.scriptFileList.getCurrentPage().getPath());
                intent.putExtra(ProjectConfigActivity.EXTRA_NEW_PROJECT, true);
                startActivity(intent);
                break;

        }
    }
}