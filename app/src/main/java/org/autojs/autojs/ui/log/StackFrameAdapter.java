package org.autojs.autojs.ui.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.autojs.autojs.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying stack frames in a list
 */
public class StackFrameAdapter extends RecyclerView.Adapter<StackFrameAdapter.ViewHolder> {

    private List<LogBottomSheet.StackFrameInfo> mFrames = new ArrayList<>();
    private OnItemClickListener mItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(LogBottomSheet.StackFrameInfo frame, int position);
    }

    public void setFrames(List<LogBottomSheet.StackFrameInfo> frames) {
        mFrames = frames != null ? frames : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stack_frame, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogBottomSheet.StackFrameInfo frame = mFrames.get(position);
        
        holder.functionName.setText(frame.functionName);
        holder.location.setText(holder.itemView.getContext().getString(
                R.string.text_stack_frame_location,
                frame.fileName,
                frame.lineNumber,
                frame.columnNumber > 0 ? ":" + frame.columnNumber : ""
        ));

        holder.itemView.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(frame, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFrames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView functionName;
        TextView location;

        ViewHolder(View itemView) {
            super(itemView);
            functionName = itemView.findViewById(R.id.tv_function_name);
            location = itemView.findViewById(R.id.tv_location);
        }
    }
}
