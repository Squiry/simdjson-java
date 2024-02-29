package org.simdjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.plokhotnyuk.jsoniter_scala.core.ReaderConfig$;
import com.github.plokhotnyuk.jsoniter_scala.core.package$;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.openjdk.jmh.annotations.*;
import org.simdjson.kora.$Twitter_JsonReader;
import org.simdjson.kora.$Twitter_Status_JsonReader;
import org.simdjson.kora.$Twitter_User_JsonReader;
import ru.tinkoff.kora.json.common.JsonCommonModule;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.ListJsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.simdjson.SimdJsonPaddingUtil.padded;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 1, time = 5)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class ParseAndSelectBenchmark {

    private final SimdJsonParser simdJsonParser = new SimdJsonParser();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonReader<org.simdjson.kora.Twitter> koraReader = new $Twitter_JsonReader(
        new ListJsonReader<>(
            new $Twitter_Status_JsonReader(
                new $Twitter_User_JsonReader()
            )
        )
    );

    private byte[] buffer;
    private byte[] bufferPadded;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        try (InputStream is = ParseBenchmark.class.getResourceAsStream("/twitter.json")) {
            buffer = is.readAllBytes();
            bufferPadded = padded(buffer);
        }
    }

    @Benchmark
    public int countUniqueUsersWithDefaultProfile_jsoniter_scala() throws IOException {
        Twitter twitter = package$.MODULE$.readFromArray(buffer, ReaderConfig$.MODULE$, Twitter$.MODULE$.codec());
        Set<String> defaultUsers = new HashSet<>();
        for (Status tweet : twitter.statuses()) {
            User user = tweet.user();
            if (user.default_profile()) {
                defaultUsers.add(user.screen_name());
            }
        }
        return defaultUsers.size();
    }

//    @Benchmark
    public int countUniqueUsersWithDefaultProfile_jackson() throws IOException {
        JsonNode jacksonJsonNode = objectMapper.readTree(buffer);
        Set<String> defaultUsers = new HashSet<>();
        Iterator<JsonNode> tweets = jacksonJsonNode.get("statuses").elements();
        while (tweets.hasNext()) {
            JsonNode tweet = tweets.next();
            JsonNode user = tweet.get("user");
            if (user.get("default_profile").asBoolean()) {
                defaultUsers.add(user.get("screen_name").textValue());
            }
        }
        return defaultUsers.size();
    }

    @Benchmark
    public int countUniqueUsersWithDefaultProfile_kora() throws IOException {
        var twitter = koraReader.read(buffer);
        Set<String> defaultUsers = new HashSet<>();
        for (var tweet : twitter.statuses()) {
            var user = tweet.user();
            if (user.defaultProfile()) {
                defaultUsers.add(user.screenName());
            }
        }
        return defaultUsers.size();
    }


    //    @Benchmark
    public int countUniqueUsersWithDefaultProfile_fastjson() {
        JSONObject jsonObject = (JSONObject) JSON.parse(buffer);
        Set<String> defaultUsers = new HashSet<>();
        Iterator<Object> tweets = jsonObject.getJSONArray("statuses").iterator();
        while (tweets.hasNext()) {
            JSONObject tweet = (JSONObject) tweets.next();
            JSONObject user = (JSONObject) tweet.get("user");
            if (user.getBoolean("default_profile")) {
                defaultUsers.add(user.getString("screen_name"));
            }
        }
        return defaultUsers.size();
    }

    //    @Benchmark
    public int countUniqueUsersWithDefaultProfile_jsoniter() {
        Any json = JsonIterator.deserialize(buffer);
        Set<String> defaultUsers = new HashSet<>();
        for (Any tweet : json.get("statuses")) {
            Any user = tweet.get("user");
            if (user.get("default_profile").toBoolean()) {
                defaultUsers.add(user.get("screen_name").toString());
            }
        }
        return defaultUsers.size();
    }

    private static final byte[] statuses = "statuses".getBytes(StandardCharsets.UTF_8);
    private static final byte[] user = "user".getBytes(StandardCharsets.UTF_8);
    private static final byte[] default_profile = "default_profile".getBytes(StandardCharsets.UTF_8);
    private static final byte[] screen_name = "screen_name".getBytes(StandardCharsets.UTF_8);

    @Benchmark
    public int countUniqueUsersWithDefaultProfile_simdjson() {
        JsonValue simdJsonValue = simdJsonParser.parse(buffer, buffer.length);
        Set<String> defaultUsers = new HashSet<>();
        Iterator<JsonValue> tweets = simdJsonValue.get(statuses).arrayIterator();
        while (tweets.hasNext()) {
            JsonValue tweet = tweets.next();
            JsonValue user = tweet.get(ParseAndSelectBenchmark.user);
            if (user.get(default_profile).asBoolean()) {
                defaultUsers.add(user.get(screen_name).asString());
            }
        }
        return defaultUsers.size();
    }

    //    @Benchmark
    public int countUniqueUsersWithDefaultProfile_simdjsonPadded() {
        JsonValue simdJsonValue = simdJsonParser.parse(bufferPadded, buffer.length);
        Set<String> defaultUsers = new HashSet<>();
        Iterator<JsonValue> tweets = simdJsonValue.get("statuses").arrayIterator();
        while (tweets.hasNext()) {
            JsonValue tweet = tweets.next();
            JsonValue user = tweet.get("user");
            if (user.get("default_profile").asBoolean()) {
                defaultUsers.add(user.get("screen_name").asString());
            }
        }
        return defaultUsers.size();
    }
}
