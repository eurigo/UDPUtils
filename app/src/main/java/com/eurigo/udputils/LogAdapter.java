package com.eurigo.udputils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Eurigo
 * Created on 2021/6/30 10:38
 * desc   :
 */
public class LogAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public LogAdapter(@Nullable List<String> data) {
        super(R.layout.item_log, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder helper, String s) {
        helper.setText(R.id.tv_item_log, s);
    }

    public void addDataAndScroll(@NotNull String data){
        addData(data);
        getRecyclerView().scrollToPosition(getData().size() -1);
    }
}
