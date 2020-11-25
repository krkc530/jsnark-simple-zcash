package dev.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.hash.SHA256Gadget;
import util.Util;

import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.math.BigInteger;

import dev.gadgets.ExportCoinGadget;


public class MintGenerator extends CircuitGenerator {

	private Wire[] addr_pk;
	private Wire v;
	private Wire[] rho, r, k, s, cm;
	private Wire apk;
	private Wire[] c;
	private SHA256Gadget sha2Gadget;
	private ExportCoinGadget ecGadget;

	public MintGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {

		Random random = new Random();
		int bitwidth = 32;

		addr_pk = createInputWireArray(2, "addr_pk");
		v = createInputWire("v");

		BigInteger rho_ = new BigInteger(256, random);
		BigInteger r_ = new BigInteger(256, random);
		BigInteger s_ = new BigInteger(256, random);
		rho = createConstantWireArray(Util.split(rho_, 8, 32), "rho");
		r = createConstantWireArray(Util.split(r_, 8, 32), "r");
		s = createConstantWireArray(Util.split(s_, 8, 32), "s");
		
		apk = addr_pk[0];
		
		Wire[] mergedWires = new Wire[17];
		System.arraycopy(rho, 0, mergedWires, 0, 8);
		System.arraycopy(r, 0, mergedWires, 8, 8);
		mergedWires[16] = apk;
		
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		k = sha2Gadget.getOutputWires();

		mergedWires = new Wire[17];
		System.arraycopy(s, 0, mergedWires, 0, 8);
		System.arraycopy(k, 0, mergedWires, 8, 8);
		mergedWires[16] = v;
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		cm = sha2Gadget.getOutputWires();

		c = new Wire[35];
		c[0] = addr_pk[0]; c[1] = addr_pk[1]; c[2] = v;
		System.arraycopy(rho, 0, c, 3, 8);
		System.arraycopy(r, 0, c, 11, 8);
		System.arraycopy(s, 0, c, 19, 8);
		System.arraycopy(cm, 0, c, 27, 8);

		makeOutputArray(c, "coin");

		ExportCoinGadget ecGadget = new ExportCoinGadget(c);
		makeOutputArray(ecGadget.getOutputWires(), "1 if coin saved");
	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {
		try{
            File file = new File("mintInput.txt");
			Scanner scan = new Scanner(file);
			String apk_str = scan.nextLine();
			String pk_enc = scan.nextLine();
			String v_str = scan.nextLine();
			circuitEvaluator.setWireValue(addr_pk[0], Integer.parseInt(apk_str));
			circuitEvaluator.setWireValue(addr_pk[1], Integer.parseInt(pk_enc));
			circuitEvaluator.setWireValue(v, Integer.parseInt(v_str));
			
        }catch (FileNotFoundException e) {
			System.err.println("'/mintInput.txt' must be exist");
			System.exit(1);
        }
	}

	public static void main(String[] args) throws Exception {

		MintGenerator generator = new MintGenerator("MINT");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
