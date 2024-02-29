package org.simdjson.kora;


import jakarta.annotation.Nullable;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonField;

import java.util.List;

@Json
public record Twitter(List<Status> statuses) {
    @Json
    public record Status(User user){}
    @Json
    public record User(@JsonField("default_profile") boolean defaultProfile, @Nullable String screenName){}
}
