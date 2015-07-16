package com.github.fbertola.motherdocker.spock

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@ExtensionAnnotation(MotherDockingExtension.class)
public @interface WithDockerConfig {

    String filename()

    String uri() default 'unix:///var/run/docker.sock'

    // TODO: certificates

}
