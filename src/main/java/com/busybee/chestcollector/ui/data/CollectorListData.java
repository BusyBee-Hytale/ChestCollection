package com.busybee.chestcollector.ui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CollectorListData {
    public static final BuilderCodec<CollectorListData> CODEC = BuilderCodec.<CollectorListData>builder(CollectorListData.class, CollectorListData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, s) -> d.action = s, d -> d.action)
            .addField(new KeyedCodec<>("Button", Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

    private String action;
    private String button;

    public String getAction() { return action; }
    public String getButton() { return button; }
}
