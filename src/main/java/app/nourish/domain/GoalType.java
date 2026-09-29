package app.nourish.domain;

/**
 * Goal type gates which features, metrics, and notification styles are available
 * (BRD §3). It is not merely a label — the recovery goal disables calorie framing
 * by default (SG-1, SG-2).
 */
public enum GoalType {
    LOSE,
    GAIN,
    MAINTAIN,
    RECOVERY
}
