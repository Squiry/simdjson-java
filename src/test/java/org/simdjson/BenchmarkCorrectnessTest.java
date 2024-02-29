package org.simdjson;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.simdjson.TestUtils.loadTestFile;
import static org.simdjson.TestUtils.padWithSpaces;
import static org.simdjson.TestUtils.toUtf8;

public class BenchmarkCorrectnessTest {

    @Test
    public void countUniqueTwitterUsersWithDefaultProfile() throws IOException {
        // given
        SimdJsonParser parser = new SimdJsonParser();
        Set<String> defaultUsers = new HashSet<>();
        byte[] json = loadTestFile("/twitter.json");

        // when
        JsonValue simdJsonValue = parser.parse(json, json.length);
        Iterator<JsonValue> tweets = simdJsonValue.get("statuses").arrayIterator();
        while (tweets.hasNext()) {
            JsonValue tweet = tweets.next();
            JsonValue user = tweet.get("user");
            if (user.get("default_profile").asBoolean()) {
                defaultUsers.add(user.get("screen_name").asString());
            }
        }

        // then
        assertThat(defaultUsers.size()).isEqualTo(86);
    }
    @Test
    public void countUniqueTwitterUsersWithDefaultProfileJackson() throws IOException {
        // given
        Set<String> defaultUsers = new HashSet<>();
        byte[] json = loadTestFile("/twitter.json");

        // when
        SerializableString statusesStr = new SerializedString("statuses");
        SerializableString userStr = new SerializedString("user");
        SerializableString defaultProfileStr = new SerializedString("default_profile");
        SerializableString screenNameStr = new SerializedString("screen_name");
        try (var parser = new ObjectMapper().createParser(json)) {
            var token = parser.nextToken();
            if (token != JsonToken.START_OBJECT) {
                parser.skipChildren();
            } else {
                while (true) {
                    if (parser.nextFieldName(statusesStr)) {
                        token = parser.nextToken();
                        if (token != JsonToken.START_ARRAY) {
                            parser.skipChildren();
                            continue;
                        }
                        // statuses
                        while (true) {
                            token = parser.nextToken();
                            if (token == JsonToken.END_ARRAY) {
                                break;
                            }
                            if (token != JsonToken.START_OBJECT) {
                                parser.skipChildren();
                                continue;
                            }
                            while (true) {
                                if (parser.nextFieldName(userStr)) {
                                    token = parser.nextToken();
                                    if (token != JsonToken.START_OBJECT) {
                                        parser.skipChildren();
                                        continue;
                                    }
                                    boolean defaultProfile = false;
                                    String screenName = null;
                                    while (true) {
                                        if (parser.nextFieldName(defaultProfileStr)) {
                                            parser.nextToken();
                                            defaultProfile = parser.getBooleanValue();
                                        } else if (parser.currentToken() == JsonToken.FIELD_NAME && parser.currentName().equals("screen_name")) {
                                            parser.nextToken();
                                            screenName = parser.getValueAsString();
                                        } else {
                                            if (parser.currentToken() == JsonToken.END_OBJECT) {
                                                break;
                                            }
                                            parser.nextToken();
                                            parser.skipChildren();
                                        }
                                    }
                                    if (defaultProfile) {
                                        defaultUsers.add(screenName);
                                    }
                                } else {
                                    System.out.println(parser.currentName());
                                    if (parser.currentToken() == JsonToken.END_OBJECT) {
                                        break;
                                    }
                                    parser.nextToken();
                                    parser.skipChildren();
                                }
                            }
                        }
                    } else {
                        if (parser.currentToken() == JsonToken.END_OBJECT) {
                            break;
                        }
                        parser.nextToken();
                        parser.skipChildren();
                    }
                }
            }
        }
        // then
        assertThat(defaultUsers.size()).isEqualTo(86);
    }

    @ParameterizedTest
    @CsvSource({
            "2.2250738585072013e-308, 0x1p-1022", // fast path
            "1.00000000000000188558920870223463870174566020691753515394643550663070558368373221972569761144603605635692374830246134201063722058e-309, 1e-309" // slow path
    })
    public void numberParserTest(String input, Double expected) {
        // given
        Tape tape = new Tape(100);
        NumberParser numberParser = new NumberParser(tape);
        byte[] numberUtf8Bytes = toUtf8(padWithSpaces(input));

        // when
        numberParser.parseNumber(numberUtf8Bytes, 0);

        // then
        assertThat(tape.getDouble(0)).isEqualTo(expected);
    }
}
