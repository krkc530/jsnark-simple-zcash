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


public class PourGenerator extends CircuitGenerator {

	private Wire[] addrOld_pk, addrOld_sk, addrNew_pk; //address info
	private Wire apk, askOld; //key info from addr info
	private Wire old_v, new_v; //coin value
	private Wire[] rho_old, r_old, s_old, cm_old, sn, coin_old, k_old; //old info
	private Wire[] cm_old_re; // compare to cm_old 
	private Wire[] rho, r, k, s, cm, coin; //new info
	private SHA256Gadget sha2Gadget;
	private ExportCoinGadget ecGadget;


	public PourGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {

		Random random = new Random();
		int bitwidth = 32;
		
		addrOld_sk = createInputWireArray(2);
		addrNew_pk = createInputWireArray(2);
		new_v = createInputWire();

		coin_old = createInputWireArray(35);
		addrOld_pk = createInputWireArray(2);
		old_v = createInputWire();
		rho_old = createInputWireArray(8);
		r_old = createInputWireArray(8);
		s_old = createInputWireArray(8);
		cm_old = createInputWireArray(8);
		
		//coin_old consistency check -> cm_old == H(s_old, H(rho, r), v)?
		Wire[] mergedWires = new Wire[17];
		System.arraycopy(rho_old, 0, mergedWires, 0, 8);
		System.arraycopy(r_old, 0, mergedWires, 8, 8);
		mergedWires[16] = addrOld_pk[0]; //apk_old
		
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		k_old = sha2Gadget.getOutputWires();

		mergedWires = new Wire[17];
		System.arraycopy(s_old, 0, mergedWires, 0, 8);
		System.arraycopy(k_old, 0, mergedWires, 8, 8);
		mergedWires[16] = old_v;
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		cm_old_re = sha2Gadget.getOutputWires();

		for (int i=0; i<8; i++) 
			addEqualityAssertion(cm_old[i], cm_old_re[i], "check cm");

		//balance check
		addEqualityAssertion(new_v, old_v, "check balance");


		askOld = addrOld_sk[0];

		mergedWires = new Wire[9];
		System.arraycopy(rho_old, 0, mergedWires, 0, 8);
		mergedWires[8] = askOld;

		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 4 * 9, false, true);
		sn = sha2Gadget.getOutputWires(); //compute sn_old

		apk = addrNew_pk[0];

		BigInteger rho_ = new BigInteger(256, random);
		BigInteger r_ = new BigInteger(256, random);
		BigInteger s_ = new BigInteger(256, random);
		rho = createConstantWireArray(Util.split(rho_, 8, 32));
		r = createConstantWireArray(Util.split(r_, 8, 32));
		s = createConstantWireArray(Util.split(s_, 8, 32));

		mergedWires = new Wire[17];
		System.arraycopy(rho, 0, mergedWires, 0, 8);
		System.arraycopy(r, 0, mergedWires, 8, 8);
		mergedWires[16] = apk;
		
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		k = sha2Gadget.getOutputWires();

		mergedWires = new Wire[17];
		System.arraycopy(s, 0, mergedWires, 0, 8);
		System.arraycopy(k, 0, mergedWires, 8, 8);
		mergedWires[16] = new_v;
		sha2Gadget = new SHA256Gadget(mergedWires, bitwidth, 68, false, true);
		cm = sha2Gadget.getOutputWires();

		coin = new Wire[35];
		coin[0] = addrNew_pk[0]; coin[1] = addrNew_pk[1]; coin[2] = new_v;
		System.arraycopy(rho, 0, coin, 3, 8);
		System.arraycopy(r, 0, coin, 11, 8);
		System.arraycopy(s, 0, coin, 19, 8);
		System.arraycopy(cm, 0, coin, 27, 8);

		makeOutputArray(coin, "new coin");
		ExportCoinGadget ecGadget = new ExportCoinGadget(coin);
		makeOutputArray(ecGadget.getOutputWires(), "1 if coin saved");

	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {
		try{
			File file = new File("pourInput.txt");
			Scanner scan;
			BigInteger num;

			scan = new Scanner(file); //parse address and new coin value
			String coinFileName = scan.nextLine();
			File coinFile = new File("./coins/" + coinFileName);

			String str = scan.nextLine();
			circuitEvaluator.setWireValue(addrOld_sk[0], Integer.parseInt(str));
			str = scan.nextLine();
			circuitEvaluator.setWireValue(addrOld_sk[1], Integer.parseInt(str));
			str = scan.nextLine();
			circuitEvaluator.setWireValue(addrNew_pk[0], Integer.parseInt(str));
			str = scan.nextLine();
			circuitEvaluator.setWireValue(addrNew_pk[1], Integer.parseInt(str));
			str = scan.nextLine();
			circuitEvaluator.setWireValue(new_v, Integer.parseInt(str));
			scan.close();
			
			scan = new Scanner(coinFile); //parse coin
			for (int i=0; i<35; i++) {
				str = scan.nextLine();
				num = new BigInteger(str);
				circuitEvaluator.setWireValue(coin_old[i], num);
				if (i==0 || i==1) circuitEvaluator.setWireValue(addrOld_pk[i], num);
				else if (i == 2) circuitEvaluator.setWireValue(old_v, num);
				else if (3 <= i && i < 11) circuitEvaluator.setWireValue(rho_old[i-3], num);
				else if (11 <= i && i < 19) circuitEvaluator.setWireValue(r_old[i-11], num);
				else if (19 <= i && i < 27) circuitEvaluator.setWireValue(s_old[i-19], num);
				else if (27 <= i) circuitEvaluator.setWireValue(cm_old[i-27], num);
			}
			scan.close();
        }catch (FileNotFoundException e) {
			System.err.println("'/pourInput.txt' or a coin does not exist");
			System.exit(1);
        }
	}

	public static void main(String[] args) throws Exception {

		PourGenerator generator = new PourGenerator("POUR");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
