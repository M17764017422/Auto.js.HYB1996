package org.autojs.autojs.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.AvatarViewBinding;
import org.autojs.autojs.network.NodeBB;
import org.autojs.autojs.network.entity.user.User;

/**
 * Created by 婷 on 2017/9/29.
 */

public class AvatarView extends FrameLayout {

    private AvatarViewBinding binding;
    private GradientDrawable mIconTextBackground;

    public AvatarView(@NonNull Context context) {
        super(context);
        init();
    }

    public AvatarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        binding = AvatarViewBinding.inflate(LayoutInflater.from(getContext()), this, true);
        mIconTextBackground = (GradientDrawable) binding.iconText.getBackground();
    }

    public void setIcon(int resId) {
        binding.icon.setVisibility(View.VISIBLE);
        binding.iconText.setVisibility(View.GONE);
        binding.icon.setImageResource(resId);
    }

    public void setUser(final User user) {
        if (TextUtils.isEmpty(user.getPicture())) {
            binding.icon.setVisibility(View.GONE);
            binding.iconText.setVisibility(View.VISIBLE);
            mIconTextBackground.setColor(Color.parseColor(user.getIconBgColor()));
            mIconTextBackground.setCornerRadius(getWidth() / 2);
            binding.iconText.setText(user.getIconText());
        } else {
            binding.icon.setVisibility(View.VISIBLE);
            binding.iconText.setVisibility(View.GONE);
            binding.icon.setCornerRadius(getWidth() / 2);
            Glide.with(getContext())
                    .load(NodeBB.BASE_URL + user.getPicture())
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                    )
                    .into(binding.icon);
        }
    }
}