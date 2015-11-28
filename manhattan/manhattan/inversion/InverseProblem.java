package manhattan.inversion;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * @author kensuke
 * @since 2015/08/08
 * @version 0.0.2
 *
 * @version 0.0.3
 * @since 2015/8/28
 * 
 * 
 * 
 * 
 * 
 */
public abstract class InverseProblem {

	protected RealMatrix ans;
	protected RealMatrix ata;
	protected RealVector atd;

	public RealMatrix getANS() {
		return ans;
	}

	abstract InverseMethodEnum getEnum();

	/**
	 * @param i
	 *            index
	 * @return i th answer
	 */
	public RealVector getAns(int i) {
		return ans.getColumnVector(i);
	}

	/**
	 * @return the number of unknown parameters
	 */
	public int getParN() {
		return ata.getColumnDimension();
	}

	/**
	 * @param sigmaD
	 *            &sigma;<sub>d</sub>
	 * @param j
	 *            index
	 * @return j番目の解の共分散行列 &sigma;<sub>d</sub> <sup>2</sup> V (&Lambda;
	 *         <sup>T</sup>&Lambda;) <sup>-1</sup> V<sup>T</sup>
	 */
	public abstract RealMatrix computeCovariance(double sigmaD, int j);

	private static void writeDat(Path out, double[] dat) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
			Arrays.stream(dat).forEach(pw::println);
		}
	}

	/**
	 * 解のアウトプット
	 * 
	 * @param outPath
	 *            {@link File} for output of solutions
	 * @throws IOException if an I/O error occurs
	 */
	public void outputAns(Path outPath) throws IOException {
		Files.createDirectories(outPath);
		System.out.println("outputting the answer files in " + outPath);
		for (int i = 0; i < getParN(); i++) {
			String ii = String.valueOf(i + 1);
			Path out = outPath.resolve(getEnum() + ii + ".txt");
			double[] m = ans.getColumn(i);
			writeDat(out, m);
		}
	}

	public abstract void compute();

	/**
	 * @return 基底ベクトルを並べた行列 i番目のcolumnに i番目の基底ベクトル
	 */
	public abstract RealMatrix getBaseVectors();

}