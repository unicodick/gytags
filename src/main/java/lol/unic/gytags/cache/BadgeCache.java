package lol.unic.gytags.cache;

import lol.unic.gytags.protocol.Protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BadgeCache {
    private volatile Map<String, List<String>> entries = Map.of();
    private volatile long revision = -1;

    public synchronized void applySnapshot(Protocol.Snapshot snapshot) {
        if (snapshot.revision() < revision) {
            return;
        }
        Map<String, List<String>> next = new HashMap<>();
        for (Protocol.Member member : snapshot.members()) {
            putOrRemove(next, member);
        }
        entries = Map.copyOf(next);
        revision = snapshot.revision();
    }

    public synchronized void applyUpdate(Protocol.Update update) {
        if (update.revision() < revision) {
            return;
        }
        Map<String, List<String>> next = new HashMap<>(entries);
        for (Protocol.Member member : update.members()) {
            putOrRemove(next, member);
        }
        entries = Map.copyOf(next);
        revision = Math.max(revision, update.revision());
    }

    public List<String> badgesFor(String nickname) {
        return entries.getOrDefault(NicknameNormalizer.normalize(nickname), List.of());
    }

    private static void putOrRemove(Map<String, List<String>> entries, Protocol.Member member) {
        String nickname = NicknameNormalizer.normalize(member.nickname());
        if (nickname.isEmpty()) {
            return;
        }
        if (!"ok".equals(member.status()) || member.badges().isEmpty()) {
            entries.remove(nickname);
        } else {
            entries.put(nickname, List.copyOf(member.badges()));
        }
    }
}
