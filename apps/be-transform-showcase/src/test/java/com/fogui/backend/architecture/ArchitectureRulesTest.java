package com.fogui.backend.architecture;

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
                        .importPackages("com.fogui.backend");

        private static final ArchRule REPOSITORIES_SHOULD_BE_INTERFACES_AND_FOLLOW_NAMING = classes()
            .that().resideInAPackage("..repository..")
            .should().beInterfaces()
                        .andShould().haveSimpleNameEndingWith("Repository")
                        .allowEmptyShould(true);

        private static final ArchRule CONTROLLERS_SHOULD_FOLLOW_NAMING_CONVENTION = classes()
            .that().resideInAPackage("..controller..")
            .should().haveSimpleNameEndingWith("Controller");

        private static final ArchRule SECURITY_FILTERS_SHOULD_EXTEND_ONCE_PER_REQUEST_FILTER = classes()
            .that().resideInAPackage("..security..")
            .and().haveSimpleNameEndingWith("Filter")
                        .should().beAssignableTo(OncePerRequestFilter.class)
                        .allowEmptyShould(true);

        private static final ArchRule SERVICES_SHOULD_NOT_DEPEND_ON_CONTROLLERS = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

        private static final ArchRule DTO_LAYER_SHOULD_NOT_DEPEND_ON_WEB_OR_REPO_LAYERS = noClasses()
            .that().resideInAPackage("..dto..")
                        .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..repository..")
                        .allowEmptyShould(true);

        @Test
        void repositoriesShouldBeInterfacesAndFollowNaming() {
                REPOSITORIES_SHOULD_BE_INTERFACES_AND_FOLLOW_NAMING.check(CLASSES);
        }

        @Test
        void controllersShouldFollowNamingConvention() {
                CONTROLLERS_SHOULD_FOLLOW_NAMING_CONVENTION.check(CLASSES);
        }

        @Test
        void securityFiltersShouldExtendOncePerRequestFilter() {
                SECURITY_FILTERS_SHOULD_EXTEND_ONCE_PER_REQUEST_FILTER.check(CLASSES);
        }

        @Test
        void servicesShouldNotDependOnControllers() {
                SERVICES_SHOULD_NOT_DEPEND_ON_CONTROLLERS.check(CLASSES);
        }

        @Test
        void dtoLayerShouldNotDependOnWebOrRepoLayers() {
                DTO_LAYER_SHOULD_NOT_DEPEND_ON_WEB_OR_REPO_LAYERS.check(CLASSES);
        }

}
