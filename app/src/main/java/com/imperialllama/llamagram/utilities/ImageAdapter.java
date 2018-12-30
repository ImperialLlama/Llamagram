package com.imperialllama.llamagram.utilities;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.imperialllama.llamagram.R;
import com.imperialllama.llamagram.activities.FeedActivity;
import com.imperialllama.llamagram.models.Image;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private ArrayList<Image> mDataset;
    private FeedActivity mActivity;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.text_view)
        public TextView mTextView;
        @BindView(R.id.image_view)
        public ImageView mImageView;
        @BindView(R.id.like_button)
        public FloatingActionButton mLikeButton;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public ImageAdapter(ArrayList<Image> mDataset, FeedActivity mActivity) {
        this.mDataset = mDataset;
        this.mActivity = mActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_view, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Image image = (Image) mDataset.get(position);
        if (image.user != null)
            holder.mTextView.setText(image.user.displayName);
        // load the image into the ImageView
        Picasso.get().load(image.downloadUrl).into(holder.mImageView);

	    if (image.hasLiked)
	    	holder.mLikeButton.setImageResource(R.drawable.ic_favorite_white_24dp);
	    else
	    	holder.mLikeButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);

        holder.mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.setLiked(image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void addImage(Image image) {
        mDataset.add(0, image);
        notifyDataSetChanged();
    }
}
