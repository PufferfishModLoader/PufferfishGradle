package me.dreamhopping.pml.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a declaration that is only present on one distribution of Minecraft.
 * <p>
 * Automatically added by PufferfishGradle during merging.
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface OnlyOn {
    /**
     * Returns which distribution type this declaration is present on.
     */
    DistributionType value();
}
