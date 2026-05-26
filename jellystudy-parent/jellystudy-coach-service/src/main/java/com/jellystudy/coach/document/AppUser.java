package com.jellystudy.coach.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "app_users")
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;
    private String displayName;
    private Date createdAt;
}
