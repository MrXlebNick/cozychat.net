package com.messiah.messenger.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.messiah.messenger.R;

/**
 * Created by XlebNick for Carousel.
 */
public abstract class LoadableFragment extends Fragment {
    private View failedLayout;
    private View progressBar;
    private LinearLayout contentContainer;
    private TextView emptyDataset;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, View content) {
        View view = inflater.inflate(R.layout.loadable_fragment, container, false);

        failedLayout = view.findViewById(R.id.loadable_fragment_failed_message);
        progressBar = view.findViewById(R.id.loadable_fragment_progress_bar);
        contentContainer = (LinearLayout) view.findViewById(R.id.loadable_fragment_content_container);
        emptyDataset = (TextView) view.findViewById(R.id.empty_dataset);
        View retryButton = view.findViewById(R.id.loadable_fragment_retry);
        retryButton.setOnClickListener(v -> load());

        contentContainer.addView(content);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return view;
    }

    public final void load() {
        emptyDataset.setVisibility(View.GONE);
        failedLayout.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        onLoadStart();
    }

    protected abstract void onLoadStart();

    @Override
    public void onStart() {
        super.onStart();
        load();
    }

    public void onLoaded() {
        emptyDataset.setVisibility(View.GONE);
        failedLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
    }

    public void onFailed() {
        emptyDataset.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        failedLayout.setVisibility(View.VISIBLE);
    }

    public void onEmpty(@Nullable String message) {
        emptyDataset.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        failedLayout.setVisibility(View.GONE);
        if (message != null && !TextUtils.isEmpty(message))
            emptyDataset.setText(message);
    }
}
