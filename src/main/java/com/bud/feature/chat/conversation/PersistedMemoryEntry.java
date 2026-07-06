package com.bud.feature.chat.conversation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;

public class PersistedMemoryEntry {

        @Nonnull
        public static final BuilderCodec<PersistedMemoryEntry> CODEC = BuilderCodec
                        .builder(PersistedMemoryEntry.class, PersistedMemoryEntry::new)
                        .append(new KeyedCodec<>("Summary", Codec.STRING),
                                        (entry, value) -> entry.summary = value != null ? value : "",
                                        entry -> entry.summary)
                        .add()
                        .append(new KeyedCodec<>("Importance", Codec.INTEGER),
                                        (entry, value) -> entry.importance = value,
                                        entry -> entry.importance)
                        .add()
                        .append(new KeyedCodec<>("EffectiveScore", Codec.DOUBLE),
                                        (entry, value) -> entry.effectiveScore = value,
                                        entry -> entry.effectiveScore)
                        .add()
                        .append(new KeyedCodec<>("SpeakerName", Codec.STRING),
                                        (entry, value) -> entry.speakerName = value != null ? value : "",
                                        entry -> entry.speakerName)
                        .add()
                        .append(new KeyedCodec<>("Mode", new EnumCodec<>(ConversationMode.class)),
                                        (entry, value) -> entry.mode = value != null ? value
                                                        : ConversationMode.DIALOG_MODE,
                                        entry -> entry.mode)
                        .add()
                        .append(new KeyedCodec<>("Participants", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                                        (entry, value) -> entry.participants = value != null ? new HashSet<>(value)
                                                        : new HashSet<>(),
                                        entry -> entry.participants)
                        .add()
                        .append(new KeyedCodec<>("CreatedAt", Codec.LONG),
                                        (entry, value) -> entry.createdAt = value,
                                        entry -> entry.createdAt)
                        .add()
                        .append(new KeyedCodec<>("Legendary", Codec.BOOLEAN),
                                        (entry, value) -> entry.legendary = value,
                                        entry -> entry.legendary)
                        .add()
                        .build();

        @Nonnull
        private String summary = "";
        private int importance;
        private double effectiveScore;
        @Nonnull
        private String speakerName = "";
        @Nonnull
        private ConversationMode mode = ConversationMode.GENERAL;
        @Nonnull
        private Set<String> participants = new HashSet<>();
        private long createdAt;
        private boolean legendary;

        public PersistedMemoryEntry() {
        }

        @Nonnull
        public static PersistedMemoryEntry from(@Nonnull ConversationMemoryEntry entry) {
                PersistedMemoryEntry persisted = new PersistedMemoryEntry();
                persisted.summary = Objects.requireNonNull(entry.summary());
                persisted.importance = entry.importance();
                persisted.effectiveScore = entry.effectiveScore();
                persisted.speakerName = Objects.requireNonNull(entry.speakerName());
                persisted.mode = Objects.requireNonNull(entry.mode());
                persisted.participants = new HashSet<>(entry.participants());
                persisted.createdAt = entry.createdAt();
                persisted.legendary = entry.legendary();
                return persisted;
        }

        @Nonnull
        public ConversationMemoryEntry toEntry() {
                return new ConversationMemoryEntry(
                                this.summary,
                                this.importance,
                                this.effectiveScore,
                                this.speakerName,
                                this.mode,
                                this.participants,
                                this.createdAt,
                                this.legendary);
        }

}
