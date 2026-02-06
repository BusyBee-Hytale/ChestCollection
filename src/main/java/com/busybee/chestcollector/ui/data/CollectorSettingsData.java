package com.busybee.chestcollector.ui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CollectorSettingsData {
    public static final BuilderCodec<CollectorSettingsData> CODEC = BuilderCodec.<CollectorSettingsData>builder(CollectorSettingsData.class, CollectorSettingsData::new)
            .addField(new KeyedCodec<>("Button", Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, s) -> d.action = s, d -> d.action)
            .addField(new KeyedCodec<>("@FilterText", Codec.STRING), (d, s) -> d.filterText = s, d -> d.filterText)
            .addField(new KeyedCodec<>("@RadiusSlider", Codec.FLOAT), (d, s) -> d.radiusSlider = s, d -> d.radiusSlider)
            .addField(new KeyedCodec<>("@NotificationTypeDropdown", Codec.STRING), (d, s) -> d.notificationTypeDropdown = s, d -> d.notificationTypeDropdown)
            .build();

    private String button;
    private String action;
    private String filterText;
    private Float radiusSlider;
    private String notificationTypeDropdown;

    public String getButton() { return button; }
    public String getAction() { return action; }
    public String getFilterText() { return filterText; }
    public Float getRadiusSlider() { return radiusSlider; }
    public String getNotificationTypeDropdown() { return notificationTypeDropdown; }
}