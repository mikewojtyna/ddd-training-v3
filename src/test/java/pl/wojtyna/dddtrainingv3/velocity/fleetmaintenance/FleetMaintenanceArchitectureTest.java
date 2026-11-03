package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// Context boundary rules for the FleetMaintenance bounded context: other contexts may depend on FleetMaintenance only through
/// its `api` package, and FleetMaintenance may depend on other contexts only through their `api` packages.
@AnalyzeClasses(packages = "pl.wojtyna.dddtrainingv3", importOptions = ImportOption.DoNotIncludeTests.class)
class FleetMaintenanceArchitectureTest {

    private static final String ROOT = "pl.wojtyna.dddtrainingv3";
    private static final String CONTEXT = ROOT + ".velocity.fleetmaintenance";

    @ArchTest
    static final ArchRule other_contexts_use_only_fleet_maintenance_api =
        noClasses().that().resideOutsideOfPackage(CONTEXT + "..")
                   .should().dependOnClassesThat(resideInAPackage(CONTEXT + "..")
                                                     .and(resideOutsideOfPackage(CONTEXT + ".api..")));

    @ArchTest
    static final ArchRule fleet_maintenance_uses_only_other_contexts_api =
        noClasses().that().resideInAPackage(CONTEXT + "..")
                   .should().dependOnClassesThat(nonApiClassOfAnotherContext());

    /// A class of another bounded context (`<root>.<system>.<context>..`) outside its `api` package.
    /// Shared code (`<root>.common..`, `<root>.<system>.common..`, `<root>.<system>.contextmap..`) is not a context.
    private static DescribedPredicate<JavaClass> nonApiClassOfAnotherContext() {
        return DescribedPredicate.describe("non-api class of another bounded context", javaClass -> {
            var pkg = javaClass.getPackageName();
            if (!pkg.startsWith(ROOT + ".") || pkg.equals(CONTEXT) || pkg.startsWith(CONTEXT + ".")) {
                return false;
            }
            var parts = pkg.substring(ROOT.length() + 1).split("\\.");
            if (parts.length < 2 || parts[0].equals("common") || Set.of("common", "contextmap").contains(parts[1])) {
                return false;
            }
            return !(parts.length >= 3 && parts[2].equals("api"));
        });
    }
}
