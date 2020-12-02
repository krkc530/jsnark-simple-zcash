package dev.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;

import dev.gadgets.MerkleTreeGadget;

public class MerkleTreeGenerator extends CircuitGenerator {

    private MerkleTreeGadget merkleTreeGadget;
    private Wire[] output;

	public MerkleTreeGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {
        merkleTreeGadget = new MerkleTreeGadget();
		output = merkleTreeGadget.getOutputWires();

		makeOutputArray(output, "Merkle Tree");
	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {
	}

	public static void main(String[] args) throws Exception {

		MerkleTreeGenerator generator = new MerkleTreeGenerator("Merkle Tree Gen");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
