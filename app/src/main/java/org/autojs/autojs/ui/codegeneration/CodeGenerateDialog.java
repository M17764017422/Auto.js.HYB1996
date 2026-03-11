package org.autojs.autojs.ui.codegeneration;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.stardust.app.DialogUtils;
import com.stardust.autojs.codegeneration.CodeGenerator;
import org.autojs.autojs.R;
import org.autojs.autojs.ui.widget.ExpandableAdapter;
import org.autojs.autojs.ui.widget.ExpandableGroup;
import org.autojs.autojs.ui.widget.CheckBoxCompat;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import com.stardust.theme.util.ListBuilder;
import com.stardust.util.ClipboardUtil;
import com.stardust.view.accessibility.NodeInfo;

import java.util.ArrayList;
import java.util.List;

import org.autojs.autojs.databinding.DialogCodeGenerateBinding;
import org.autojs.autojs.databinding.DialogCodeGenerateOptionBinding;

/**
 * Created by Stardust on 2017/11/6.
 */

public class CodeGenerateDialog extends ThemeColorMaterialDialogBuilder {

    private final List<OptionGroup> mOptionGroups = new ListBuilder<OptionGroup>()
            .add(new OptionGroup(R.string.text_options, false)
                    .addOption(R.string.text_using_id_selector, true)
                    .addOption(R.string.text_using_text_selector, true)
                    .addOption(R.string.text_using_desc_selector, true))
            .add(new OptionGroup(R.string.text_select)
                    .addOption(R.string.text_find_one, true)
                    .addOption(R.string.text_until_find)
                    .addOption(R.string.text_wait_for)
                    .addOption(R.string.text_selector_exists))
            .add(new OptionGroup(R.string.text_action)
                    .addOption(R.string.text_click)
                    .addOption(R.string.text_long_click)
                    .addOption(R.string.text_set_text)
                    .addOption(R.string.text_scroll_forward)
                    .addOption(R.string.text_scroll_backward))
            .list();

    private DialogCodeGenerateBinding binding;
    private NodeInfo mRootNode;
    private NodeInfo mTargetNode;
    private Adapter mAdapter;

    public CodeGenerateDialog(@NonNull Context context, NodeInfo rootNode, NodeInfo targetNode) {
        super(context);
        mRootNode = rootNode;
        mTargetNode = targetNode;
        positiveText(R.string.text_generate);
        negativeText(R.string.text_cancel);
        onPositive(((dialog, which) -> generateCodeAndShow()));
        setupViews();
    }

    private void generateCodeAndShow() {
        String code = generateCode();
        if (code == null) {
            Toast.makeText(getContext(), R.string.text_generate_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        DialogUtils.showDialog(new ThemeColorMaterialDialogBuilder(getContext())
                .title(R.string.text_generated_code)
                .content(code)
                .positiveText(R.string.text_copy)
                .onPositive(((dialog, which) -> ClipboardUtil.setClip(getContext(), code)))
                .build());
    }


    private String generateCode() {
        CodeGenerator generator = new CodeGenerator(mRootNode, mTargetNode);
        OptionGroup settings = getOptionGroup(R.string.text_options);
        generator.setUsingId(settings.getOption(R.string.text_using_id_selector).checked);
        generator.setUsingText(settings.getOption(R.string.text_using_text_selector).checked);
        generator.setUsingDesc(settings.getOption(R.string.text_using_desc_selector).checked);
        generator.setSearchMode(getSearchMode());
        setAction(generator);
        return generator.generateCode();
    }

    private void setAction(CodeGenerator generator) {
        OptionGroup action = getOptionGroup(R.string.text_action);
        if (action.getOption(R.string.text_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
        if (action.getOption(R.string.text_long_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
        }
        if (action.getOption(R.string.text_scroll_forward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
        }
        if (action.getOption(R.string.text_scroll_backward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
        }
    }

    private int getSearchMode() {
        OptionGroup selectMode = getOptionGroup(R.string.text_select);
        if (selectMode.getOption(R.string.text_find_one).checked) {
            return CodeGenerator.FIND_ONE;
        }
        if (selectMode.getOption(R.string.text_until_find).checked) {
            return CodeGenerator.UNTIL_FIND;
        }
        if (selectMode.getOption(R.string.text_wait_for).checked) {
            return CodeGenerator.WAIT_FOR;
        }
        if (selectMode.getOption(R.string.text_selector_exists).checked) {
            return CodeGenerator.EXISTS;
        }
        return CodeGenerator.FIND_ONE;
    }

    private void setupViews() {
        binding = DialogCodeGenerateBinding.inflate(LayoutInflater.from(context));
        customView(binding.getRoot(), false);
        binding.options.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new Adapter(mOptionGroups);
        binding.options.setAdapter(mAdapter);
    }


    private OptionGroup getOptionGroup(int title) {
        for (OptionGroup group : mOptionGroups) {
            if (group.titleRes == title) {
                return group;
            }
        }
        throw new IllegalArgumentException();
    }


    private void uncheckOthers(int parentAdapterPosition, Option child) {
        boolean notify = false;
        for (Option other : child.group.options) {
            if (other != child) {
                if (other.checked) {
                    other.checked = false;
                    notify = true;
                }
            }
        }
        if (notify)
            mAdapter.notifyParentChanged(parentAdapterPosition);
    }

    private static class Option {
        int titleRes;
        boolean checked;
        OptionGroup group;

        Option(int titleRes, boolean checked) {
            this.titleRes = titleRes;
            this.checked = checked;
        }

    }

    private static class OptionGroup implements ExpandableGroup<Option> {
        int titleRes;
        List<Option> options = new ArrayList<>();
        private boolean mExpanded;

        OptionGroup(int titleRes, boolean initialExpanded) {
            this.titleRes = titleRes;
            mExpanded = initialExpanded;
        }

        OptionGroup(int titleRes) {
            this(titleRes, true);
        }

        Option getOption(int titleRes) {
            for (Option option : options) {
                if (option.titleRes == titleRes) {
                    return option;
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public List<Option> getChildren() {
            return options;
        }

        @Override
        public boolean isExpanded() {
            return mExpanded;
        }

        @Override
        public void setExpanded(boolean expanded) {
            mExpanded = expanded;
        }

        @Override
        public boolean isInitiallyExpanded() {
            return mExpanded;
        }

        OptionGroup addOption(int titleRes) {
            return addOption(titleRes, false);
        }

        OptionGroup addOption(int res, boolean checked) {
            Option option = new Option(res, checked);
            option.group = this;
            options.add(option);
            return this;
        }
    }


    private class OptionGroupViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        ImageView icon;

        OptionGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(view -> {
                int groupPosition = getAdapterPosition();
                if (groupPosition != RecyclerView.NO_POSITION && groupPosition < mOptionGroups.size()) {
                    OptionGroup group = mOptionGroups.get(groupPosition);
                    mAdapter.toggleGroup(group);
                    icon.setRotation(mAdapter.isExpanded(group) ? 0 : -90);
                }
            });
        }
    }

    class OptionViewHolder extends RecyclerView.ViewHolder {

        private DialogCodeGenerateOptionBinding binding;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = DialogCodeGenerateOptionBinding.bind(itemView);
            itemView.setOnClickListener(view -> binding.checkbox.toggle());
            binding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int groupPosition = -1;
                int childPosition = getAdapterPosition();
                // 找到对应的 group
                for (int i = 0; i < mOptionGroups.size(); i++) {
                    OptionGroup group = mOptionGroups.get(i);
                    if (childPosition < group.getChildren().size()) {
                        groupPosition = i;
                        break;
                    }
                    childPosition -= group.getChildren().size();
                }
                Option option = getOptionAt(getAdapterPosition());
                if (option != null) {
                    option.checked = isChecked;
                    if (isChecked && option.group.titleRes != R.string.text_options)
                        uncheckOthers(groupPosition, option);
                }
            });
        }

        private Option getOptionAt(int position) {
            for (OptionGroup group : mOptionGroups) {
                if (position < group.getChildren().size()) {
                    return group.getChildren().get(position);
                }
                position -= group.getChildren().size();
            }
            return null;
        }
    }

    private class Adapter extends ExpandableAdapter<OptionGroup, Option, OptionGroupViewHolder, OptionViewHolder> {

        public Adapter(@NonNull List<OptionGroup> parentList) {
            super(parentList);
        }

        @NonNull
        @Override
        protected OptionGroupViewHolder onCreateGroupViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OptionGroupViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_code_generate_option_group, parent, false));
        }

        @NonNull
        @Override
        protected OptionViewHolder onCreateChildViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OptionViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_code_generate_option, parent, false));
        }

        @Override
        protected void onBindGroupViewHolder(@NonNull OptionGroupViewHolder viewHolder, @NonNull OptionGroup optionGroup, int groupPosition) {
            viewHolder.title.setText(optionGroup.titleRes);
            viewHolder.icon.setRotation(mAdapter.isExpanded(optionGroup) ? 0 : -90);
        }

        @Override
        protected void onBindChildViewHolder(@NonNull OptionViewHolder viewHolder, @NonNull Option option, int groupPosition, int childPosition) {
            viewHolder.binding.title.setText(option.titleRes);
            viewHolder.binding.checkbox.setChecked(option.checked, false);
        }
    }

}