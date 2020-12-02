package dev.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import util.Util;
import circuit.config.Config;
import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.math.BigInteger;

import dev.gadgets.ExportCoinGadget;
import dev.gadgets.SubsetSumHashGadget;
import dev.gadgets.MerkleTreePathAlterGadget;


public class PourGenerator extends CircuitGenerator {

	private Wire[] addrOld_pk, addrOld_sk, addrNew_pk; //address info
	private Wire apk, askOld; //key info from addr info
	private Wire old_v, new_v; //coin value
	private Wire rho_old, r_old, s_old, cm_old, sn, k_old; //old info
	private Wire[] coin_old, coin;
	private Wire cm_old_calc; // compare to cm_old 
	private Wire rho, r, k, s, cm; //new info
	private Wire root, path;
	
	private ExportCoinGadget ecGadget;
	private MerkleTreePathAlterGadget merkleTreeGadget;


	public PourGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {		
		addrOld_sk = createInputWireArray(2);
		addrNew_pk = createInputWireArray(2);
		new_v = createInputWire();
		coin_old = createInputWireArray(7);

		addrOld_pk = new Wire[2];
		addrOld_pk[0] = coin_old[0]; addrOld_pk[1] = coin_old[1];
		old_v = coin_old[2];
		rho_old = coin_old[3];
		r_old = coin_old[4];
		s_old = coin_old[5];
		cm_old = coin_old[6];
		
		//coin_old consistency check -> cm_old == H(s_old, H(rho, r), v)?
		k_old = getHash(rho_old, r_old, addrOld_pk[0]);
		cm_old_calc = getHash(s_old, k_old, old_v);
		addEqualityAssertion(cm_old, cm_old_calc, "check cm");

		//balance check -> old_v == new_v?
		addEqualityAssertion(new_v, old_v, "check balance");

		//path check
		root = createInputWire();
		path = createInputWire();
		merkleTreeGadget = new MerkleTreePathAlterGadget(path, cm_old);
		Wire actualRoot = merkleTreeGadget.getOutputWires()[0];
		addEqualityAssertion(actualRoot, root, "check path");


		askOld = addrOld_sk[0];

		sn = getHash(rho_old, askOld);

		apk = addrNew_pk[0];

		rho = createInputWire();
		r = createInputWire();
		s = createInputWire();

		k = getHash(rho, r, apk);
		cm = getHash(s, k, new_v);

		coin = new Wire[7];
		coin[0] = addrNew_pk[0]; coin[1] = addrNew_pk[1]; coin[2] = new_v;
		coin[3] = rho; coin[4] = r;  coin[5] = s; coin[6] = cm;

		makeOutputArray(coin, "new coin");
		ExportCoinGadget ecGadget = new ExportCoinGadget(coin);
		makeOutputArray(ecGadget.getOutputWires(), "1 if coin saved");

	}

	private static BigInteger nextRandomBigInteger(BigInteger n) { // for randomized variable
		Random rand = new Random();  // system time seed
		BigInteger result = new BigInteger(n.bitLength(), rand);
		while (result.compareTo(n) >= 0) { // to prevent overflow
			result = new BigInteger(n.bitLength(), rand);
		}
		return result;
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
			File inputfile = new File("pourInput.txt");
			Scanner scan;
			
			scan = new Scanner(inputfile); // parse address and new coin value
			String coinFileName = scan.nextLine();
			String str;
			str = scan.nextLine(); circuitEvaluator.setWireValue(addrOld_sk[0], Integer.parseInt(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(addrOld_sk[1], Integer.parseInt(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(addrNew_pk[0], Integer.parseInt(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(addrNew_pk[1], Integer.parseInt(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(new_v, Integer.parseInt(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(root, new BigInteger(str));
			str = scan.nextLine(); circuitEvaluator.setWireValue(path, Integer.parseInt(str));
			scan.close();
			
			File coinFile = new File("./coins/" + coinFileName);
			BigInteger num;
			scan = new Scanner(coinFile); // parse old coin
			for (int i=0; i<7; i++) {
				str = scan.nextLine();
				num = new BigInteger(str);
				circuitEvaluator.setWireValue(coin_old[i], num);
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
