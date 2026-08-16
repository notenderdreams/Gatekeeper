package com.gatekeeper;

public final class AutoTesterTest {
    private AutoTesterTest() {}

    public static void main(String[] args) {
        CircuitRecipe nand = CircuitRecipe.all().get(0);
        CircuitRecipe nor = CircuitRecipe.all().get(1);
        WorkbenchGraph graph = new WorkbenchGraph(nand);
        AutoTester tester = new AutoTester();

        require(tester.start(nand), "a tester sweep should start for a target config");
        AutoTester.Tick matching = finishSweep(tester, graph);
        require(matching.finished() && matching.passed(),
            "the NAND graph should pass the independent NAND target config");
        Boolean[] observations = tester.observations();
        require(Boolean.TRUE.equals(observations[0])
                && Boolean.TRUE.equals(observations[1])
                && Boolean.TRUE.equals(observations[2])
                && Boolean.FALSE.equals(observations[3]),
            "the tester should retain all four observed graph outputs");

        require(tester.start(nor), "the target can change without reloading the graph");
        AutoTester.Tick mismatching = finishSweep(tester, graph);
        require(mismatching.finished() && !mismatching.passed(),
            "the unchanged NAND graph should fail the NOR target config");
        require(graph.outputValue(0, new boolean[] { false, true }),
            "selecting a tester target must leave the workbench graph unchanged");

        require(tester.start(nand), "a new sweep should start after completion");
        require(tester.stop(), "stop should halt an active sweep");
        require(!tester.isRunning(), "a stopped tester must be idle");
        tester.clearResults();
        for (Boolean observation : tester.observations()) {
            require(observation == null, "clear should remove every recorded result");
        }

        System.out.println("AutoTesterTest: all checks passed");
    }

    private static AutoTester.Tick finishSweep(AutoTester tester, WorkbenchGraph graph) {
        AutoTester.Tick tick = AutoTester.Tick.IDLE;
        for (int frame = 0; frame < 4 * 36; frame++) {
            tick = tester.update(graph);
        }
        return tick;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
