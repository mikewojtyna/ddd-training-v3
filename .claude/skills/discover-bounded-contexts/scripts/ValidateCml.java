import org.contextmapper.dsl.contextMappingDSL.BoundedContext;
import org.contextmapper.dsl.contextMappingDSL.ContextMappingModel;
import org.contextmapper.dsl.contextMappingDSL.CustomerSupplierRelationship;
import org.contextmapper.dsl.contextMappingDSL.Partnership;
import org.contextmapper.dsl.contextMappingDSL.SharedKernel;
import org.contextmapper.dsl.contextMappingDSL.SymmetricRelationship;
import org.contextmapper.dsl.contextMappingDSL.UpstreamDownstreamRelationship;
import org.contextmapper.dsl.standalone.ContextMapperStandaloneSetup;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates a Context Mapper (.cml) file in two layers.
 * 1. ERROR/WARNING lines: syntax and Context Mapper's own semantic checks. Errors make the file unusable.
 * 2. RULE lines: smells against the context-map rules in references/context-map-rules.md
 *    (shared kernel, partnership, upstream/downstream cycles, downstream without ACL, conformists to check).
 *    They are input for the critic, not verdicts: some have legitimate reasons, which the report must state.
 * Exit code: 0 = no errors (RULE lines may exist), 1 = errors, 2 = usage.
 */
public class ValidateCml {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ValidateCml <file.cml>");
            System.exit(2);
        }
        var file = new File(args[0]);
        if (!file.isFile()) {
            System.err.println("no such file: " + file);
            System.exit(2);
        }
        var cml = ContextMapperStandaloneSetup.getStandaloneAPI().loadCML(file);
        var resource = cml.getXtextResource();
        var issues = resource.getResourceServiceProvider()
                             .getResourceValidator()
                             .validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        int errors = 0;
        for (var issue : issues) {
            if (issue.getSeverity() == Severity.ERROR) {
                errors++;
            }
            System.out.printf("%s line %d: %s%n", issue.getSeverity(), issue.getLineNumber(), issue.getMessage());
        }
        var model = cml.getContextMappingModel();
        int contexts = model == null ? 0 : model.getBoundedContexts().size();
        int relationships = model == null || model.getMap() == null ? 0 : model.getMap().getRelationships().size();
        if (errors == 0 && model != null && model.getMap() != null) {
            lintRules(model);
        }
        System.out.printf("%s: %d bounded contexts, %d relationships, %d errors%n",
                          errors == 0 ? "VALID" : "INVALID", contexts, relationships, errors);
        System.exit(errors == 0 ? 0 : 1);
    }

    private static void lintRules(ContextMappingModel model) {
        var map = model.getMap();
        Map<String, Set<String>> upstreamToDownstreams = new TreeMap<>();
        Set<String> related = new HashSet<>();
        System.out.println("RELATIONSHIPS:");
        for (var relationship : map.getRelationships()) {
            String label = relationship.getName() == null ? "" : " : " + relationship.getName();
            if (relationship instanceof SymmetricRelationship symmetric) {
                String a = name(symmetric.getParticipant1());
                String b = name(symmetric.getParticipant2());
                related.add(a);
                related.add(b);
                if (relationship instanceof SharedKernel) {
                    System.out.printf("  %s [SK]<->[SK] %s%s%n", a, b, label);
                    System.out.printf("RULE shared-kernel: %s <-> %s. Almost always wrong (synchronisation, "
                                      + "context-specific rules leak into the 'common' code).%n", a, b);
                } else if (relationship instanceof Partnership) {
                    System.out.printf("  %s [P]<->[P] %s%s%n", a, b, label);
                    System.out.printf("RULE partnership: %s <-> %s. Fine only in an early stage; prefer "
                                      + "upstream/downstream once the boundary is established.%n", a, b);
                }
            } else if (relationship instanceof UpstreamDownstreamRelationship ud) {
                String up = name(ud.getUpstream());
                String down = name(ud.getDownstream());
                related.add(up);
                related.add(down);
                upstreamToDownstreams.computeIfAbsent(up, k -> new TreeSet<>()).add(down);
                var upRoles = ud.getUpstreamRoles().stream().map(r -> r.getLiteral()).collect(Collectors.toList());
                var downRoles = ud.getDownstreamRoles().stream().map(r -> r.getLiteral()).collect(Collectors.toList());
                boolean cs = relationship instanceof CustomerSupplierRelationship;
                System.out.printf("  %s %s -> %s %s%s%s%n", up, upRoles, down, downRoles, cs ? " (customer/supplier)" : "", label);
                if (downRoles.contains("CF")) {
                    System.out.printf("RULE conformist-check: %s conforms to %s. OK only if %s's language is "
                                      + "well established; otherwise it pollutes %s.%n", down, up, up, down);
                }
                if (!downRoles.contains("ACL") && !downRoles.contains("CF") && cs) {
                    System.out.printf("RULE no-acl-customer: %s is a customer of %s without an ACL. Acceptable when "
                                      + "the customer really shapes the supplier's model; otherwise add [ACL] "
                                      + "(Context Mapper only warns about ACL in customer/supplier).%n", down, up);
                } else if (!downRoles.contains("ACL") && !downRoles.contains("CF")) {
                    System.out.printf("RULE no-acl: %s is downstream of %s without an ACL. An ACL is almost "
                                      + "always a good idea downstream.%n", down, up);
                }
            }
        }
        for (var cycle : cycles(upstreamToDownstreams)) {
            if (cycle.size() == 3) {
                System.out.printf("RULE ud-cycle: %s. Upstream/downstream going both ways is almost always a "
                                  + "design error.%n", String.join(" -> ", cycle));
            } else {
                System.out.printf("RULE ud-cycle-indirect: %s. Check whether each context really depends on the "
                                  + "next one's model, or whether one arrow is only event flow drawn the wrong way.%n",
                                  String.join(" -> ", cycle));
            }
        }
        for (var context : map.getBoundedContexts()) {
            if (!related.contains(name(context))) {
                System.out.printf("RULE isolated: %s has no relationship. Separate ways is acceptable only for a "
                                  + "deliberate R&D silo.%n", name(context));
            }
        }
    }

    /** Elementary cycles in the upstream->downstream graph, each reported once (rotated to its smallest name). */
    private static List<List<String>> cycles(Map<String, Set<String>> graph) {
        Set<List<String>> found = new LinkedHashSet<>();
        for (var start : graph.keySet()) {
            walk(graph, start, start, new ArrayList<>(List.of(start)), found);
        }
        return new ArrayList<>(found);
    }

    private static void walk(Map<String, Set<String>> graph, String start, String current, List<String> path,
                             Set<List<String>> found) {
        for (var next : graph.getOrDefault(current, Set.of())) {
            if (next.equals(start)) {
                var cycle = new ArrayList<>(path);
                int min = cycle.indexOf(Collections.min(cycle));
                Collections.rotate(cycle, -min);
                cycle.add(cycle.get(0));
                found.add(cycle);
            } else if (!path.contains(next) && next.compareTo(start) > 0) {
                path.add(next);
                walk(graph, start, next, path, found);
                path.remove(path.size() - 1);
            }
        }
    }

    private static String name(BoundedContext context) {
        return context == null ? "?" : context.getName();
    }
}
