package shark.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides service's alternative name which should be registered to Shark Framework
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SharkServiceAlternativeName {
    /**
     * Alternative names of the service
     * @return service aliases
     */
    String[] value();
}
