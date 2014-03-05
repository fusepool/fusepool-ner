package eu.fusepool.enhancer.engines.ners;

import java.security.Timestamp;

/**
 * Represents an extracted entity.
 * @author Gábor Reményi
 */
public class Entity {
    String model;
    // begin position in text
    int begin;
    // end position in text
    int end;
    // label of the entity
    String label;
    // type of the entity
    String type;
    // confidence score
    double score;
    // time of textraction
    Timestamp timestamp;

    public Entity() {
    }

    public Entity(int begin, int end, String label, String type, Timestamp timestamp) {
        this.begin = begin;
        this.end = end;
        this.label = label;
        this.type = type;
        this.timestamp = timestamp;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
