package com.activiti.extension.bean.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Incentro
 * <p>
 * Annotate methods with this annotation and one or several groupnames (i.e. FUNCTIONEEL_BEHEER or PROCES_REGISSEURS)
 * to state which authorities can utilize said methods. The name of the group is equal to the APS name. As you can see
 * in the example above you don't need to state the full APS name (i.e. FUNC_PROCES_REGISSEURS_MO) this way you can
 * specify groups cluster independently.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authority {

    public String[] requiredAuthorities();
}
