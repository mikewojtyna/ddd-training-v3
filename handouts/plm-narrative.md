# Corvane Drive Systems: how we work

*Corvane is a fictional company. This is the head of engineering talking to a new platform team that will replace Corvane's patchwork of tools.*

We build electric actuator units for tractors, excavators and harvesters. Our customers are the machine makers, the OEMs. We sell them two things: the units themselves, and engineering analysis. We simulate how a unit, or sometimes their whole subassembly, behaves under load, heat and vibration.

## Our units

We don't have a catalogue of finished parts. We have about forty base designs, and every OEM order picks options: stroke length, motor power, connector type, sealing class, mounting flange. Not every combination works. A high-power motor needs the large housing, and the highest sealing class isn't available with the quick connector. Sales configures the unit together with the customer, and the configurator checks those rules.

Every unit is a structure of assemblies and parts, down to the screws: the bill of materials. One base design with its options gives thousands of possible BOMs, but we only ever build a few hundred of them. We don't keep units in stock; every unit is built to order. When an OEM wants something outside the rules, say a flange nobody has made before, it's no longer a configuration. Engineering has to design it, and that goes through the change board.

The BOM is a sore point. Engineering owns the BOM in the PLM system, but our plant in Brno keeps its own version with packaging, glue and the fixtures they need on the line. Engineering says it's the same BOM with extras. Brno says it's a different BOM. Every few months they disagree about which one is right.

## Simulations

Our analysis team has a library of simulation templates: "thermal run-in", "flange fatigue", "drop test", "EMC pre-check". A template fixes the mesh rules, the load cases, the solver settings and the acceptance criteria. An engineer starts a simulation run from a template and fills in the parameters for a concrete unit: which BOM, which load profile, which ambient temperature. The run goes to the compute cluster. When the results come back, a second engineer reviews them and either signs them off or sends them back.

Templates change. Last spring we found that "flange fatigue" underestimated the stress at the bolt holes, so we released version 4 of the template. Auditors always ask the same question: which version did that run from two years ago use? We must be able to answer, and we must never upgrade an old run silently. If somebody repeats an old run with the new version, it's a new run with its own results, not a correction of the old one. Not everyone agrees: some engineers want to fix the old run and keep its number, because the customer's report refers to it.

## Changes

Nothing in a released design changes without the engineering change board. Anyone can raise an engineering change request: a plant, a supplier, a customer, our own engineers. The request says what should change, why, and which units and BOMs are affected.

Every Thursday the board looks at the open requests. It has five voting members: design, simulation, quality, purchasing, and the manager of the affected plant. A decision needs at least four members present and a simple majority. Quality can veto anything that touches safety-relevant parts. If the vote is tied, or a request stays undecided for three meetings, it goes up to the chief engineer, who decides alone. A rejected request can come back only with new information. Urgent changes, for example when the line in Brno has stopped, can be approved by two members by phone, but the full board must confirm them at the next meeting. Sometimes it doesn't, and then we have built units with a change that was never approved.

## Customers, suppliers, plants

We deal with many companies, and the same company often shows up in more than one way. Tillgard is our biggest customer for units, but they also supply us with cast housings, and they buy simulation studies for their own gearboxes. Our plant in Brno builds units. Our plant in Porto builds units too, and also runs the vibration test lab, which our other departments order from as if it were an external supplier. People matter as well: every customer has a technical contact who may raise change requests and a purchasing contact who signs contracts. Sometimes that's the same person. Today every tool keeps its own customer list, and Tillgard exists in ours six times, with three different addresses.

## Selling analysis

Simulation studies are sold separately from units. We have a price list per template and complexity: a standard thermal run-in has a fixed price, a fatigue study is priced per load case. Large OEMs have frame contracts with their own prices and a yearly volume discount. Rush jobs delivered within 48 hours cost 40% more. If a run has to be repeated because of our own mistake, like the old flange template, the customer doesn't pay again. Who decides what counts as our mistake is a long story.

## Everything else

Engineers keep CAD models and simulation result files in the vault, a big file store with check-in and check-out. We also have a knowledge base of lessons learned: short articles about failures and how we fixed them. Everybody agrees both are important. Nobody agrees who owns them.
