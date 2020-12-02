package dev.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import circuit.config.Config;
import util.Util;
import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.math.BigInteger;

import dev.gadgets.ExportCoinGadget;
import dev.gadgets.SubsetSumHashGadget;

public class MintGenerator extends CircuitGenerator {

	private Wire[] addr_pk;
	private Wire value;
	private Wire rho, r, k, s, cm;
	private Wire apk;
	private Wire[] coin;
	private SubsetSumHashGadget subsetSumGadget;
	private ExportCoinGadget ecGadget;

	public MintGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {

		// Input Wire Define
		addr_pk = createInputWireArray(2, "addr_pk");
		value = createInputWire("value");
		rho = createInputWire("rho");
		r = createInputWire("r");
		s = createInputWire("s");
		
		apk = addr_pk[0]; // parse
		
		k = getHash(rho, r, apk);
		cm = getHash(s, k, value);

		coin = new Wire[7]; // coin define
		coin[0] = addr_pk[0]; coin[1] = addr_pk[1]; coin[2] = value;
		coin[3] = rho; coin[4] = r;  coin[5] = s; coin[6] = cm;

		makeOutputArray(coin, "coin"); // make output

		ExportCoinGadget ecGadget = new ExportCoinGadget(coin); //save coin as file
		makeOutputArray(ecGadget.getOutputWires(), "coin saved if one");
	}

	private Wire getHash(Wire... args) { // variable args length
		int argLen = args.length;
		Wire[] inHash = new Wire[argLen];
		for (int i=0; i<argLen; i++) {
			inHash[i] = args[i];
		}

		Wire[] nextInputBits = new WireArray(inHash).getBits(Config.LOG2_FIELD_PRIME).asArray(); // bitify array
		SubsetSumHashGadget subsetSumGadget = new SubsetSumHashGadget(nextInputBits, false); // get hash
		return subsetSumGadget.getOutputWires()[0];
	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) { // read file input
		
		circuitEvaluator.setWireValue(rho, nextRandomBigInteger(Config.FIELD_PRIME)); 
		circuitEvaluator.setWireValue(r, nextRandomBigInteger(Config.FIELD_PRIME));
		circuitEvaluator.setWireValue(s, nextRandomBigInteger(Config.FIELD_PRIME));
		try{
            File file = new File("mintInput.txt");
			Scanner scan = new Scanner(file);
			String apk_str = scan.nextLine();
			String pk_enc = scan.nextLine();
			String v_str = scan.nextLine();
			circuitEvaluator.setWireValue(addr_pk[0], Integer.parseInt(apk_str));
			circuitEvaluator.setWireValue(addr_pk[1], Integer.parseInt(pk_enc));
			circuitEvaluator.setWireValue(value, Integer.parseInt(v_str));
			
        }catch (FileNotFoundException e) {
			System.err.println("'/mintInput.txt' must be exist");
			System.exit(1);
        }
	}

	private static BigInteger nextRandomBigInteger(BigInteger n) { // for randomized variable
		Random rand = new Random();  // system time seed
		BigInteger result = new BigInteger(n.bitLength(), rand);
		while (result.compareTo(n) >= 0) { // to prevent overflow
			result = new BigInteger(n.bitLength(), rand);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		MintGenerator generator = new MintGenerator("Mint Generator");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
