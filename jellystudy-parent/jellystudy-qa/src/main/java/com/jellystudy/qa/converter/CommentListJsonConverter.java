package com.jellystudy.qa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.qa.entity.Answer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class CommentListJsonConverter implements AttributeConverter<List<Answer.Comment>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Answer.Comment> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<Answer.Comment> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<Answer.Comment>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
