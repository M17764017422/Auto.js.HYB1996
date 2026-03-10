package com.stardust.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.databinding.OperationDialogItemBinding;

import java.util.ArrayList;

/**
 * Created by Stardust on 2017/6/26.
 */

public class OperationDialogBuilder extends MaterialDialog.Builder {

    private RecyclerView mOperations;
    private ArrayList<Integer> mIds = new ArrayList<>();
    private ArrayList<Integer> mIcons = new ArrayList<>();
    private ArrayList<String> mTexts = new ArrayList<>();
    private View.OnClickListener mOnItemClickListener;

    public OperationDialogBuilder(@NonNull Context context) {
        super(context);
        mOperations = new RecyclerView(context);
        mOperations.setLayoutManager(new LinearLayoutManager(context));
        mOperations.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ViewHolder holder = new ViewHolder(OperationDialogItemBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
                holder.itemView.setOnClickListener(v -> {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onClick(v);
                    }
                });
                return holder;
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
        });
        customView(mOperations, false);
    }

    public OperationDialogBuilder item(int id, int iconRes, int textRes) {
        return item(id, iconRes, getContext().getString(textRes));
    }

    public OperationDialogBuilder item(int id, int iconRes, String text) {
        mIds.add(id);
        mIcons.add(iconRes);
        mTexts.add(text);
        return this;
    }

    public OperationDialogBuilder bindItemClick(View.OnClickListener listener) {
        mOnItemClickListener = listener;
        return this;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        OperationDialogItemBinding binding;

        public ViewHolder(OperationDialogItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
