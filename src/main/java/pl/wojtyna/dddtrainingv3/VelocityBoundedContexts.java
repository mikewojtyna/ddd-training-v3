package pl.wojtyna.dddtrainingv3;

import org.contextmapper.dsl.cml.CMLResource;
import org.contextmapper.dsl.generator.ContextMapGenerator;
import org.contextmapper.dsl.generator.contextmap.ContextMapFormat;
import org.contextmapper.dsl.standalone.ContextMapperStandaloneSetup;

public class VelocityBoundedContexts {

    private static final String INPUT_FILE = "src/main/cml/VeloCity-Bounded-Contexts.cml";
    private static final String OUTPUT_DIR = "target";

    static void main() {
        var generator = new ContextMapGenerator();
        if (!generator.isGraphvizInstalled()) {
            throw new IllegalStateException("Graphviz 'dot' executable not found on PATH");
        }
        generator.setContextMapFormats(ContextMapFormat.PNG);

        var api = ContextMapperStandaloneSetup.getStandaloneAPI();
        CMLResource cml = api.loadCML(INPUT_FILE);
        api.callGenerator(cml, generator, OUTPUT_DIR);

        System.out.println("Context map PNG generated in " + OUTPUT_DIR);
    }

}