package com.vic17usd.galaxycraft.morphology;

/** Estado visual transitorio: nunca decide impactos, recompensas ni movimiento del servidor. */
public record AlienPose(int action, long start, int duration) {
  public static final int IDLE = 0,
      ATTACK = 1,
      SPIT = 2,
      BURROW = 3,
      GLIDE = 4,
      ROAR = 5,
      FEED = 6,
      TRADE = 7;
  public static final AlienPose REST = new AlienPose(IDLE, 0, 1);

  public boolean valid() {
    return action >= 0 && action <= TRADE && duration >= 1 && duration <= 200;
  }

  public float progress(double time) {
    return (float) Math.clamp((time - start) / duration, 0, 1);
  }

  public float envelope(double time) {
    return time < start || time > start + duration ? 0 : (float) Math.sin(progress(time) * Math.PI);
  }
}
