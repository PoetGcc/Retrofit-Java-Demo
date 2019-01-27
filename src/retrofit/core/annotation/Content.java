package retrofit.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author g&c
 * @date 2019-01-20
 * 内容注解
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Content {
}
