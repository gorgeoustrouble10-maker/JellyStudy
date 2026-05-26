package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetStateDTO implements Serializable {

    private String userId;
    private String petName;
    private String species;
    private int level;
    private int experience;
    private int experienceToNext;
    private String mood;
    private String appearance;
    /** 等级原始形象（不受主题皮肤影响） */
    private String baseAppearance;
    private String baseAppearanceLabel;
    private java.util.List<String> unlockedThemes;
    private String currentTheme;
}
