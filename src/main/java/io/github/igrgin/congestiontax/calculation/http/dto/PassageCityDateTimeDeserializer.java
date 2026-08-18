package io.github.igrgin.congestiontax.calculation.http.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public final class PassageCityDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    public PassageCityDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
        }

        var timestamp = parser.getString();
        try {
            return LocalDateTime.parse(timestamp, FORMAT);
        } catch (DateTimeParseException exception) {
            throw InvalidFormatException.from(
                            parser,
                            "A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss.",
                            timestamp,
                            LocalDateTime.class)
                    .withCause(exception);
        }
    }
}
