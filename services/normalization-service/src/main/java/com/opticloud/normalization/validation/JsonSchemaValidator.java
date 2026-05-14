package com.opticloud.normalization.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class JsonSchemaValidator {

    private final ObjectMapper objectMapper;
    private JsonSchema schema;

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws Exception {
        try (InputStream is = new ClassPathResource("schemas/raw-report.schema.json").getInputStream()) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(is);
        }
    }

    public ValidationOutcome validate(JsonNode payload) {
        Set<ValidationMessage> errors = schema.validate(payload);
        if (errors.isEmpty()) {
            return new ValidationOutcome(true, java.util.List.of());
        }
        return new ValidationOutcome(
                false,
                errors.stream().map(ValidationMessage::getMessage).collect(Collectors.toList()));
    }

    public record ValidationOutcome(boolean valid, java.util.List<String> errors) {}
}
