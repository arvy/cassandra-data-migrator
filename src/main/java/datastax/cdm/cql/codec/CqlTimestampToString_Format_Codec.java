package datastax.cdm.cql.codec;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import datastax.cdm.cql.CqlHelper;
import datastax.cdm.properties.KnownProperties;
import datastax.cdm.properties.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesProvider;

/**
 * This codec converts a CQL TIMESTAMP to a Java String with format specified at
 * KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT using the zone specified at
 * KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT_ZONE.
 */
public class CqlTimestampToString_Format_Codec extends AbstractBaseCodec<String> {
    public Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final DateTimeFormatter formatter;
    private final ZoneOffset zoneOffset;

    public CqlTimestampToString_Format_Codec(PropertyHelper propertyHelper, CqlHelper cqlHelper) {
        super(propertyHelper, cqlHelper);

        if (cqlHelper.isCodecRegistered(Codecset.CQL_TIMESTAMP_TO_STRING_MILLIS))
            throw new RuntimeException("Codec " + Codecset.CQL_TIMESTAMP_TO_STRING_MILLIS + " is already registered");

        String formatString = propertyHelper.getString(KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT);
        if (formatString == null || formatString.isEmpty()) {
            throw new IllegalArgumentException("Property " + KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT + " is required and cannot be empty.");
        }
        this.formatter = DateTimeFormatter.ofPattern(formatString);

        String zone = propertyHelper.getString(KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT_ZONE);
        if (zone == null || !ZoneRulesProvider.getAvailableZoneIds().contains(zone)) {
            throw new IllegalArgumentException("Property " + KnownProperties.TRANSFORM_CODECS_TIMESTAMP_STRING_FORMAT_ZONE + " is required and must be a valid ZoneOffset.");
        }
        this.zoneOffset = ZoneId.of(zone).getRules().getOffset(Instant.now());
    }

    @Override
    public @NotNull GenericType<String> getJavaType() {
        return GenericType.STRING;
    }

    @Override
    public @NotNull DataType getCqlType() {
        return DataTypes.TIMESTAMP;
    }

    @Override
    public ByteBuffer encode(String value, @NotNull ProtocolVersion protocolVersion) {
        if (value == null) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
        Instant instantValue = localDateTime.toInstant(zoneOffset);
        return TypeCodecs.TIMESTAMP.encode(instantValue, protocolVersion);
    }

    @Override
    public String decode(ByteBuffer bytes, @NotNull ProtocolVersion protocolVersion) {
        Instant instantValue = TypeCodecs.TIMESTAMP.decode(bytes, protocolVersion);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instantValue, zoneOffset);
        return formatter.format(localDateTime);
    }

    @Override
    public @NotNull String format(String value) {
        LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
        String formattedValue = formatter.format(localDateTime);
        return formattedValue;
    }

    @Override
    public String parse(String value) {
        LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
        Instant instantValue = localDateTime.toInstant(zoneOffset);
        String rtn = String.valueOf(instantValue.toEpochMilli());
        return rtn;
    }
}

