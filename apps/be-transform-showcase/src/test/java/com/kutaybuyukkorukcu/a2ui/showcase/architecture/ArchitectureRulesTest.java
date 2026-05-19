package com.kutaybuyukkorukcu.a2ui.showcase.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.kutaybuyukkorukcu.a2ui.showcase");

    private static final ArchRule CONTROLLERS_SHOULD_FOLLOW_NAMING_CONVENTION = classes()
            .that().resideInAPackage("..controller..")
            .should().haveSimpleNameEndingWith("Controller");

    private static final ArchRule SERVICES_SHOULD_NOT_DEPEND_ON_CONTROLLERS = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);

    private static final ArchRule CONFIG_SHOULD_NOT_DEPEND_ON_CONTROLLERS = noClasses()
            .that().resideInAPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);

    @Test
    void controllersShouldFollowNamingConvention() {
        CONTROLLERS_SHOULD_FOLLOW_NAMING_CONVENTION.check(CLASSES);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        SERVICES_SHOULD_NOT_DEPEND_ON_CONTROLLERS.check(CLASSES);
    }

    @Test
    void configShouldNotDependOnControllers() {
        CONFIG_SHOULD_NOT_DEPEND_ON_CONTROLLERS.check(CLASSES);
    }
}