package org.odyssey.adapter;
/**
 * Interface used for speed optimizations on asynchronous cover loading.
 * This is necessary to load covers only at certain scroll speed to not put
 * to much load on the CPU during scrolling.
 */
public interface ScrollSpeedAdapter {
    void setScrollSpeed(int speed);
}
