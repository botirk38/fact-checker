package com.fact_checker.FactChecker.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

@Converter(autoApply = true)
public class VectorConverter implements AttributeConverter<double[], String> {

    @Override
    public String convertToDatabaseColumn(double[] attribute) {
        if (attribute == null) {
            return null;
        }
        return "[" + Arrays.stream(attribute)
                .mapToObj(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("") + "]";
    }

    @Override
    public double[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.equals("[]")) {
            return new double[0];
        }
        String[] values = dbData.substring(1, dbData.length() - 1).split(",");
        return Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
    }
}
