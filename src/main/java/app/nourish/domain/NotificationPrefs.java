package app.nourish.domain;

/**
 * Per-type notification opt-in state with persona-based defaults (FR-24, SG-2).
 * For the RECOVERY goal, calorie-budget notifications default OFF and require an
 * explicit, acknowledged opt-in.
 */
public class NotificationPrefs {
    private String id;
    private String userId;

    private boolean calorieBudget;      // SG-2: off by default for RECOVERY
    private boolean mealPrepReminder = true;
    private boolean hydrationReminder = true;
    private boolean groceryDayReminder = true;

    private String quietHoursStart; // "22:00"
    private String quietHoursEnd;   // "07:00"

    // Web Push subscription (VAPID) — stored per user, opaque to the rest of the app.
    private String pushEndpoint;
    private String pushP256dh;
    private String pushAuth;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isCalorieBudget() { return calorieBudget; }
    public void setCalorieBudget(boolean v) { this.calorieBudget = v; }
    public boolean isMealPrepReminder() { return mealPrepReminder; }
    public void setMealPrepReminder(boolean v) { this.mealPrepReminder = v; }
    public boolean isHydrationReminder() { return hydrationReminder; }
    public void setHydrationReminder(boolean v) { this.hydrationReminder = v; }
    public boolean isGroceryDayReminder() { return groceryDayReminder; }
    public void setGroceryDayReminder(boolean v) { this.groceryDayReminder = v; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String v) { this.quietHoursStart = v; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String v) { this.quietHoursEnd = v; }
    public String getPushEndpoint() { return pushEndpoint; }
    public void setPushEndpoint(String v) { this.pushEndpoint = v; }
    public String getPushP256dh() { return pushP256dh; }
    public void setPushP256dh(String v) { this.pushP256dh = v; }
    public String getPushAuth() { return pushAuth; }
    public void setPushAuth(String v) { this.pushAuth = v; }
}
