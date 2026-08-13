package org.debentialc.scoreboard.animation;

import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardAnimation {

    public enum Type {
        STATIC,
        FRAMES,
        SCROLL
    }

    private final String originalText;
    private final Type type;
    private final int interval;
    private final int width;
    private final List<String> frames;

    private int tickCounter = 0;
    private int frameIndex = 0;
    private int scrollIndex = 0;

    public ScoreboardAnimation(String originalText, Type type, int interval, int width) {
        this(originalText, type, interval, width, new ArrayList<String>());
    }

    public ScoreboardAnimation(String originalText, Type type, int interval, int width, List<String> frames) {
        this.originalText = originalText != null ? originalText : "";
        this.type = type != null ? type : Type.STATIC;
        this.interval = interval > 0 ? interval : 1;
        this.width = width > 0 ? width : 26;
        this.frames = frames != null ? new ArrayList<>(frames) : new ArrayList<String>();
    }

    public String getCurrentFrame() {
        switch (type) {
            case FRAMES:
                if (frames.isEmpty()) return CC.translate(originalText);
                return CC.translate(frames.get(frameIndex % frames.size()));
            case SCROLL:
                return CC.translate(getScrollFrame());
            case STATIC:
            default:
                return CC.translate(originalText);
        }
    }

    public void tick() {
        tickCounter++;
        if (tickCounter < interval) return;
        tickCounter = 0;

        switch (type) {
            case FRAMES:
                frameIndex++;
                if (frameIndex >= frames.size()) frameIndex = 0;
                break;
            case SCROLL:
                scrollIndex++;
                String plain = CC.translate(originalText);
                if (scrollIndex >= plain.length() + width) scrollIndex = 0;
                break;
        }
    }

    private String getScrollFrame() {
        String plain = CC.translate(originalText);
        if (plain.length() <= width) {
            return plain;
        }
        int len = plain.length();
        int start = scrollIndex % (len + width);
        // Create a virtual string that loops back to the beginning
        String extended = plain + "    " + plain;
        int offset = start;
        if (offset > len) {
            offset = offset - (len + 4);
            if (offset < 0) offset = 0;
        }
        int end = Math.min(offset + width, extended.length());
        String result = extended.substring(offset, end);
        if (result.length() < width) {
            result = result + plain.substring(0, Math.min(width - result.length(), plain.length()));
        }
        return result;
    }

    public Type getType() {
        return type;
    }

    public int getInterval() {
        return interval;
    }

    public List<String> getFrames() {
        return new ArrayList<>(frames);
    }

    public String getOriginalText() {
        return originalText;
    }

    public void reset() {
        tickCounter = 0;
        frameIndex = 0;
        scrollIndex = 0;
    }
}
