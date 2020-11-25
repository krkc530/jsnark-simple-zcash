package dev.gadgets;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.auxiliary.LongElement;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

public class ExportCoinGadget extends Gadget {

	private Wire[] coin;
	private Wire output;

	public ExportCoinGadget(Wire[] coin, String...desc) {
		super(desc);
		this.coin = coin;
		buildCircuit();
	}

	private void buildCircuit() {
		output = generator.createProverWitnessWire("saving coin... : 1 if done");
		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				evaluator.setWireValue(output, 0);
				try {
					File dir = new File("./coins/");
					if (!dir.isDirectory()) {
						dir.mkdir();
					}
					int idx = 0;
					File coinFile = new File("./coins/C" + idx);
					while (coinFile.exists()) {
						idx++;
						coinFile = new File("./coins/C" + idx);
					}
					PrintWriter writer = new PrintWriter("./coins/C" + idx);
					for (int i=0; i<coin.length; i++) {
						BigInteger tmp = evaluator.getWireValue(coin[i]);
						writer.println(Long.toString(tmp.longValue()));
					}
					writer.close();
					evaluator.setWireValue(output, 1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { output };
	}
}