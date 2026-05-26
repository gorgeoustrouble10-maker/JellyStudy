package com.jellystudy.coach.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pet_states")
public class PetState {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;
    private String petName;
    private String species;
    private int level;
    private int experience;
    private String mood;
    private java.util.List<String> unlockedThemes;
    private String currentTheme;
}
