package com.example.demo.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;


@Setter
@Getter
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;
    private String name;
    private String department;

    @Field(targetType = FieldType.DECIMAL128)

    private BigDecimal salary;


}