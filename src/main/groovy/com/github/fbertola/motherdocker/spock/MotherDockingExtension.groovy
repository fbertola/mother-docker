package com.github.fbertola.motherdocker.spock

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IInterceptable
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.SpecInfo

class MotherDockingExtension extends AbstractAnnotationDrivenExtension<WithDockerConfig> {

    public MotherDockingExtension() {
        ExpandoMetaClass.enableGlobally()
    }

    void visitSpecAnnotation(WithDockerConfig annotation, SpecInfo spec) {
        addInterceptor(annotation, spec.getBottomSpec());
    }

    void visitFeatureAnnotation(WithDockerConfig annotation, FeatureInfo feature) {
        addInterceptor(annotation, feature.getFeatureMethod());
    }

    void visitFixtureAnnotation(WithDockerConfig annotation, MethodInfo fixtureMethod) {
        addInterceptor(annotation, fixtureMethod);
    }

    void addInterceptor(WithDockerConfig annotation, IInterceptable interceptable) {
        interceptable.addInterceptor(new MotherDockingInterceptor(annotation));
    }

}
