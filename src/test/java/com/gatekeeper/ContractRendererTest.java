package com.gatekeeper;

import java.awt.Font;
import java.util.List;

public final class ContractRendererTest {
    public static void main(String[] args) {
        ContractModel model = new ContractModel();
        List<ContractModel.Contract> contracts = model.available(2);
        require(contracts.size() == 3, "chapter 2 should have 3 contracts");

        ContractModel.Contract nandContract = contracts.get(0);
        require(nandContract.product().equals("NAND"), "first contract should be NAND");
        require(nandContract.requiredCount() == 3, "NAND contract should require 3 units");
        require(nandContract.unitReward() == 30, "NAND unit reward should be 30C");
        require(nandContract.totalReward() == 90, "NAND total reward should be 90C");

        require(!model.isCompleted(nandContract), "contract should start uncompleted");
        require(model.remaining(nandContract) == 3, "initial remaining quota should be 3");

        model.deliver(nandContract, 2);
        require(model.deliveries(nandContract) == 2, "deliveries should be 2 after pushing 2 units");
        require(model.remaining(nandContract) == 1, "remaining quota should be 1");
        require(!model.isCompleted(nandContract), "contract should still be incomplete with 2/3");

        model.deliver(nandContract, 1);
        require(model.deliveries(nandContract) == 3, "deliveries should be 3");
        require(model.remaining(nandContract) == 0, "remaining quota should be 0");
        require(model.isCompleted(nandContract), "contract should be marked completed");

        // Test matching crafted detection
        CraftedCircuitInventory inventory = new CraftedCircuitInventory();
        WorkbenchGraph nandGraph = new WorkbenchGraph(CircuitRecipe.all().get(0));
        inventory.add("MYNAND", nandGraph.snapshot());
        require(ContractRenderer.countMatchingCrafted(nandContract, inventory) == 1,
            "countMatchingCrafted should detect 1 matching valid NAND IC");
        require(ContractRenderer.firstMatchingCrafted(nandContract, inventory) != null,
            "firstMatchingCrafted should return the matching crafted circuit");

        // Test renderer action hit testing
        ContractRenderer renderer = new ContractRenderer(null, null, new Font("Monospaced", Font.PLAIN, 12));
        require(renderer.actionAt(300, 160, contracts) == ContractRenderer.Action.DECREASE_AMOUNT,
            "clicking decrease button should trigger DECREASE_AMOUNT");
        require(renderer.actionAt(370, 160, contracts) == ContractRenderer.Action.INCREASE_AMOUNT,
            "clicking increase button should trigger INCREASE_AMOUNT");
        require(renderer.actionAt(400, 160, contracts) == ContractRenderer.Action.MAX_AMOUNT,
            "clicking max button should trigger MAX_AMOUNT");
        require(renderer.actionAt(250, 195, contracts) == ContractRenderer.Action.DELIVER,
            "clicking deliver button should trigger DELIVER");
        require(renderer.actionAt(380, 195, contracts) == ContractRenderer.Action.CLOSE,
            "clicking close button should trigger CLOSE");
        require(renderer.actionAt(50, 60, contracts) == ContractRenderer.Action.SELECT_ORDER,
            "clicking first order row should trigger SELECT_ORDER");
        require(renderer.orderIndexAt(50, 60, contracts) == 0,
            "order index at row 0 should be 0");

        System.out.println("ContractRendererTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
