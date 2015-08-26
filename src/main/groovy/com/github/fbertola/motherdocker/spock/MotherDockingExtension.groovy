package com.github.fbertola.motherdocker.spock

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IInterceptable
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.SpecInfo

class MotherDockingExtension extends AbstractAnnotationDrivenExtension<WithDockerConfig> {

    void visitSpecAnnotation(WithDockerConfig annotation, SpecInfo spec) {
        addInterceptor(annotation, spec.getBottomSpec().g);
    }

    void visitFeatureAnnotation(WithDockerConfig annotation, FeatureInfo feature) {
        addInterceptor(annotation, feature.getFeatureMethod());
    }

    void visitFixtureAnnotation(WithDockerConfig annotation, MethodInfo fixtureMethod) {
        addInterceptor(annotation, fixtureMethod);
    }

    static void addInterceptor(WithDockerConfig annotation, IInterceptable interceptable) {
        interceptable.addInterceptor(new MotherDockingInterceptor(annotation));
    }

}
