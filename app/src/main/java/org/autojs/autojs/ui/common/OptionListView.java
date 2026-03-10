package org.autojs.autojs.ui.common;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.OptionListViewBinding;
import org.autojs.autojs.databinding.OperationDialogItemBinding;

import java.util.ArrayList;

/**
 * Created by Stardust on 2017/10/20.
 */

public class OptionListView extends LinearLayout {


    public static class Builder {

        private OptionListView mOptionListView;
        private Context mContext;

        public Builder(Context context) {
            mContext = context;
            mOptionListView = (OptionListView) View.inflate(context, R.layout.option_list_view, null);
        }

        public Builder item(int id, int iconRes, int textRes) {
            return item(id, iconRes, mContext.getString(textRes));
        }

        public Builder item(int id, int iconRes, String text) {
            mOptionListView.mIds.add(id);
            mOptionListView.mIcons.add(iconRes);
            mOptionListView.mTexts.add(text);
            return this;
        }

        public Builder title(String title) {
            mOptionListView.mTitleView.setVisibility(VISIBLE);
            mOptionListView.mTitleView.setText(title);
            return this;
        }

        public Builder title(int title) {
            return title(mContext.getString(title));
        }

        public OptionListView build() {
            return mOptionListView;
        }
    }


    private ArrayList<Integer> mIds = new ArrayList<>();
    private ArrayList<Integer> mIcons = new ArrayList<>();
    private ArrayList<String> mTexts = new ArrayList<>();
    private RecyclerView mOptionList;
    private TextView mTitleView;
    private OptionListViewBinding binding;

    public OptionListView(Context context) {
        super(context);
    }

    public OptionListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding = OptionListViewBinding.bind(this);
        mTitleView = binding.title;
        mOptionList = binding.list;
        mOptionList.setLayoutManager(new LinearLayoutManager(getContext()));
        mOptionList.setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(OperationDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.itemView.setId(mIds.get(position));
            holder.binding.text.setText(mTexts.get(position));
            holder.binding.icon.setImageResource(mIcons.get(position));
        }

        @Override
        public int getItemCount() {
            return mIds.size();
        }
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        OperationDialogItemBinding binding;

        public ViewHolder(OperationDialogItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
