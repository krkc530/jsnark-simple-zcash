package dev.gadgets;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.structure.WireArray;

import circuit.config.Config;
import util.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.io.PrintWriter;
import java.util.Scanner;
import dev.gadgets.SubsetSumHashGadget;

public class MerkleTreeGadget extends Gadget {
    private static int digestWidth = SubsetSumHashGadget.DIMENSION;
    private Wire[] commits;
    private Wire[] allHashList;
    private Wire[] hashList;
    private Wire currentHash;
    private SubsetSumHashGadget subsetSumGadget;

	public MerkleTreeGadget(String...desc) {
		super(desc);
		buildCircuit();
	}

	private void buildCircuit() {
		commits = getCommits(); // init hashes
        allHashList = commits; // insert commits
        hashList = getNewHashList(commits); // get next targets
        allHashList = Util.concat(hashList, allHashList); // save old and append new hashes >> to make merkle tree
        while (hashList.length != 1) { // got root hash
            hashList = getNewHashList(hashList);
            allHashList = Util.concat(hashList, allHashList);
        }

        generator.specifyProverWitnessComputation(new Instruction() { // make file output
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				try {
                    PrintWriter writer = new PrintWriter("./merkle_tree");
                    writer.println(allHashList.length);
					for (int i=0; i<allHashList.length; i++) {
						BigInteger tmp = evaluator.getWireValue(allHashList[i]);
						writer.println(tmp.toString());
					}
                    writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
    }
    
    private Wire[] getNewHashList(Wire[] tempHashList) { // get next targets
        Wire[] newHashList = new Wire[(tempHashList.length + 1) / 2]; // 4 -> 2 -> 1, 3 -> 2-> 1 (zero padding)
        int index = 0;

        while (index < tempHashList.length) { // foreach elements
            Wire[] inHash = new Wire[2];
			// left
			inHash[0] = tempHashList[index];
			index++;

			// right
			inHash[1] = generator.getZeroWire(); // zero padding
			if (index != tempHashList.length) { // if commit[index] exist?
				inHash[1] = tempHashList[index];
			}
            
            Wire[] nextInputBits = new WireArray(inHash).getBits(Config.LOG2_FIELD_PRIME).asArray(); 
			subsetSumGadget = new SubsetSumHashGadget(nextInputBits, false); // get hash
            currentHash = subsetSumGadget.getOutputWires()[0];
            newHashList[index / 2] = currentHash; // will have half length (binary tree)
			index++;
        }
		return newHashList;
	}
	
	private Wire[] getCommits() { // read file input
		int idx = 0;
		Scanner scanner;
		String cm = "";
		Wire[] cms;
		File coinDir = new File("./coins");
		int cmLength = coinDir.listFiles().length;
		double log2 = Math.log(cmLength) / Math.log(2);
		while (log2 > (int)log2) {
			cmLength++;
			log2 = Math.log(cmLength) / Math.log(2);
		}
		cms = new Wire[cmLength];
		for (int i=0; i < cmLength; i++) cms[i] = generator.getZeroWire();

		File coin = new File("./coins/C" + idx);
		while (coin.exists()) {
			try {
				scanner = new Scanner(coin);
				for (int i=0; i<7; i++) cm = scanner.nextLine();
				cms[idx] = cms[idx].add(new BigInteger(cm));
				idx++;
				coin = new File("./coins/C" + idx);
			} catch (FileNotFoundException e) {
				System.err.println("coin doesnt exist");
				System.exit(1);
			}
		}
		return cms;
	}

	@Override
	public Wire[] getOutputWires() {
		return allHashList; // for circuit output
	}
}