package lol.unic.gutags.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public final class Protocol {
    public static final int VERSION = 1;

    private static final Gson GSON = new Gson();

    private Protocol() {
    }

    public static String hello(List<String> nicknames) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "hello");
        message.addProperty("protocol_version", VERSION);
        message.add("nicknames", GSON.toJsonTree(nicknames));
        return GSON.toJson(message);
    }

    public static String subscribe(List<String> nicknames) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "subscribe");
        message.add("nicknames", GSON.toJsonTree(nicknames));
        return GSON.toJson(message);
    }

    public static ServerMessage parseServerMessage(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        String type = getString(object, "type", "");
        return switch (type) {
            case "hello_ack" -> new HelloAck(getInt(object, "protocol_version", -1));
            case "snapshot" -> new Snapshot(requiredLong(object, "revision"), members(object));
            case "update" -> new Update(requiredLong(object, "revision"), members(object));
            case "error" -> new Error(
                    getString(object, "code", "unknown"),
                    getString(object, "message", "")
            );
            default -> new Unknown(type);
        };
    }

    private static List<Member> members(JsonObject object) {
        if (!object.has("members") || !object.get("members").isJsonArray()) {
            throw new JsonParseException("members must be an array");
        }
        List<Member> members = GSON.fromJson(object.get("members"), new TypeToken<List<Member>>() {
        }.getType());
        if (members == null) {
            throw new JsonParseException("members must not be null");
        }
        return List.copyOf(members);
    }

    private static String getString(JsonObject object, String name, String fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : fallback;
    }

    private static int getInt(JsonObject object, String name, int fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsInt() : fallback;
    }

    private static long requiredLong(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new JsonParseException(name + " is required");
        }
        return object.get(name).getAsLong();
    }

    public sealed interface ServerMessage permits HelloAck, Snapshot, Update, Error, Unknown {
    }

    public record HelloAck(int protocolVersion) implements ServerMessage {
    }

    public record Snapshot(long revision, List<Member> members) implements ServerMessage {
    }

    public record Update(long revision, List<Member> members) implements ServerMessage {
    }

    public record Error(String code, String message) implements ServerMessage {
    }

    public record Unknown(String type) implements ServerMessage {
    }

    public record Member(String nickname, String status, List<String> badges) {
        public Member {
            nickname = nickname == null ? "" : nickname;
            status = status == null ? "" : status;
            badges = badges == null ? List.of() : List.copyOf(badges);
        }
    }
}
