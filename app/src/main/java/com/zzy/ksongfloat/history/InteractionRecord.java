package com.zzy.ksongfloat.history;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "interaction_records", indices = {@Index(value = "profileFingerprint", unique = true), @Index("nickname"), @Index("interactionStatus")})
public class InteractionRecord {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String profileFingerprint = "";
    public String nickname = "";
    public String visibleBio = "";
    public String visibleSongs = "";
    public String pageType = "UNKNOWN";
    public long firstSeenAt;
    public long lastSeenAt;
    public long lastAnalyzedAt;
    public String generatedComments = "[]";
    public String generatedMessages = "[]";
    public String interactionStatus = InteractionStatus.DISCOVERED;
    public String userNotes = "";
    public String detectedLocationText = "";
    public String riskFlags = "[]";
}
