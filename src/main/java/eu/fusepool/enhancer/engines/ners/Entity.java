/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.ners;

import java.security.Timestamp;

/**
 *
 * @author Gabor
 */
public class Entity {
    String model;
    int begin;
    int end;
    String entity;
    String type;
    Timestamp timestamp;

    public Entity() {
    }

    public Entity(int begin, int end, String entity, String type, Timestamp timestamp) {
        this.begin = begin;
        this.end = end;
        this.entity = entity;
        this.type = type;
        this.timestamp = timestamp;
    }
    
    
}
