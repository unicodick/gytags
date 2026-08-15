package lol.unic.gytags.cache;

import lol.unic.gytags.protocol.Protocol;

import java.util.HashMap;
import java.util.Map;

public final class BadgeCache {
    private volatile Map<String, String> entries = Map.of();
    private volatile long revision = -1;

    public synchronized void applySnapshot(Protocol.Snapshot snapshot) {
        if (snapshot.revision() < revision) {
            return;
        }
        Map<String, String> next = new HashMap<>();
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
        Map<String, String> next = new HashMap<>(entries);
        for (Protocol.Member member : update.members()) {
            putOrRemove(next, member);
        }
        entries = Map.copyOf(next);
        revision = Math.max(revision, update.revision());
    }

    public String badgeFor(String nickname) {
        return entries.get(NicknameNormalizer.normalize(nickname));
    }

    public long revision() {
        return revision;
    }

    private static void putOrRemove(Map<String, String> entries, Protocol.Member member) {
        String nickname = NicknameNormalizer.normalize(member.nickname());
        if (nickname.isEmpty()) {
            return;
        }
        if (!"ok".equals(member.status()) || member.badges().isEmpty()) {
            entries.remove(nickname);
        } else {
            entries.put(nickname, member.badges().getFirst());
        }
    }
}
