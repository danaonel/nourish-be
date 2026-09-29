package app.nourish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nourish — AI Personal Nutritionist backend.
 *
 * <p>MongoDB is the intended primary store (see APP-TECHNICAL-PLAN.md §3.2). It is
 * activated only under the {@code mongo} Spring profile; the default profile
 * excludes Mongo auto-configuration (see application.yml) and uses an in-memory
 * store so the service boots with no external infrastructure.</p>
 */
@SpringBootApplication
public class NourishApplication {
    public static void main(String[] args) {
        SpringApplication.run(NourishApplication.class, args);
    }
}
