package io.github.juliocastro.sepainst.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Architecture rules, checked on every build. They keep the hexagonal layering honest:
 * dependencies point inwards (adapters → application → domain) and the core stays framework-free.
 */
class ArchitectureTest {

    private static final String ROOT = "io.github.juliocastro.sepainst";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void layersOnlyDependInwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain").definedBy(ROOT + ".domain..")
                .layer("Application").definedBy(ROOT + ".application..")
                .layer("Adapters").definedBy(ROOT + ".adapter..")
                .layer("Config").definedBy(ROOT + ".config..")
                .whereLayer("Config").mayNotBeAccessedByAnyLayer()
                .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Config")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Config")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Config")
                .check(CLASSES);
    }

    @Test
    void domainAndApplicationAreFreeOfFrameworks() {
        noClasses()
                .that().resideInAnyPackage(ROOT + ".domain..", ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.fasterxml..", "org.postgresql..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void inboundAndOutboundAdaptersAreIndependent() {
        noClasses()
                .that().resideInAPackage(ROOT + ".adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.out..")
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses()
                .that().resideInAPackage(ROOT + ".adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.in..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
