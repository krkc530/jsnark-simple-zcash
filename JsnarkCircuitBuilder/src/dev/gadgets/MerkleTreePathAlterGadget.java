package dev.gadgets;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.config.Config;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.math.BigInteger;
import dev.gadgets.SubsetSumHashGadget;


public class MerkleTreePathAlterGadget extends Gadget {
	private int treeHeight;
	private Wire directionSelectorWire;
	private Wire[] directionSelectorBits;
	private Wire leafWire;
	private Wire[] intermediateHashWires;
	private Wire outRoot;
	private SubsetSumHashGadget subsetSumGadget;


	public MerkleTreePathAlterGadget(Wire directionSelectorWire, Wire leafWire, String... desc) {

		super(desc);
		this.directionSelectorWire = directionSelectorWire;
		this.leafWire = leafWire;

		buildCircuit();

	}

	private void buildCircuit() {
		Wire[] merkleTree = getMerkleTree(); // read merkle_tree file
		int treeHeight = (int)(Math.log(merkleTree.length) / Math.log(2)); // get tree height
		intermediateHashWires = generator.createProverWitnessWireArray(treeHeight);	
		generator.specifyProverWitnessComputation(new Instruction() {		// get intermediate wire array
			@Override
			public void evaluate(CircuitEvaluator evaluator) {	
				for (int i=0; i<treeHeight; i++) {
					evaluator.setWireValue(intermediateHashWires[i], 0); // init
				}
				int index = evaluator.getWireValue(directionSelectorWire).intValue();
				index += (int)Math.pow(2, treeHeight);
				for (int i=0; i<treeHeight; i++) {
					BigInteger tmp;
					if (index % 2 > 0) {
						tmp = evaluator.getWireValue(merkleTree[index-2]);
					}
					else {
						tmp = evaluator.getWireValue(merkleTree[index]);
					}
					evaluator.setWireValue(intermediateHashWires[i], tmp);
					index /= 2;
				}
			}
		});

		directionSelectorBits = directionSelectorWire.getBitWires(treeHeight).asArray(); // wire bitify, little endian (Ex: 8 -> 0001)
		Wire currentHash = leafWire; // first hash input

		// Apply CRH across tree path guided by the direction bits
		for (int i = 0; i < treeHeight; i++) {
			Wire[] inHash = new Wire[2];
			Wire temp = currentHash.sub(intermediateHashWires[i]);
			Wire temp2 = directionSelectorBits[i].mul(temp);
			inHash[1] = intermediateHashWires[i].add(temp2);	
			Wire temp3 = currentHash.add(intermediateHashWires[i]);
			inHash[0] = temp3.sub(inHash[1]);
			// define left or right
			// if direction is odd, inHash = intermediate, currentHash
			// if even, inHash = currentHash, intermediate

			Wire[] nextInputBits = new WireArray(inHash).getBits(Config.LOG2_FIELD_PRIME).asArray();
			subsetSumGadget = new SubsetSumHashGadget(nextInputBits, false);	// get hash from wire array
			currentHash = subsetSumGadget.getOutputWires()[0];
		}
		outRoot = currentHash; // merkle tree root
	}

	private Wire[] getMerkleTree() { // read merkle_tree file and return wire array
		File mtFile = new File("merkle_tree");
		Wire[] merkleTree = new Wire[2];
		try{
			Scanner scan = new Scanner(mtFile);
			String nodeNum = scan.nextLine();
			int mtNum = Integer.parseInt(nodeNum);
			merkleTree = new Wire[mtNum];
			String str = "";
			for (int i=0; i < mtNum; i++) {
				merkleTree[i] = generator.getZeroWire();  // init
				str = scan.nextLine();
				merkleTree[i] = merkleTree[i].add(new BigInteger(str));
			}			
		} catch (FileNotFoundException e) {
			System.err.println("merkle tree doesn't exist");
			System.exit(1);
		}
		return merkleTree;
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { outRoot };
	}

}
