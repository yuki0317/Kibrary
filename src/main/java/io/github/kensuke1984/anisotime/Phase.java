package io.github.kensuke1984.anisotime;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Phase name.
 * </p>
 * 
 * This class is <b>immutable</b>.
 * <p>
 * Waveform is now digitalized. Waveform is divided into parts. each part has up
 * or downgoing, P or S and the partition in which waveform exists.
 * 
 * ???PdiffXX and ???SdiffXX can be used. XX is positive double XX is
 * diffractionAngle diff must be the last part.
 * 
 * 
 * If there is a number (n) in the name. the next letter to the number will be
 * repeated n times. (e.g. S3KS &rarr; SKKKS)
 * 
 * @version 0.1.3.1
 * 
 *          TODO TauP
 * 
 * @author Kensuke Konishi
 * 
 */
public class Phase {

	// no use letters
	private static final Pattern others = Pattern.compile("[abeghj-oqrt-zA-HL-OQ-RT-Z]");
	private static final Pattern number = Pattern.compile("\\d+");

	// first letter is sSpP
	private static final Pattern firstLetter = Pattern.compile("^[^psSP]");
	// static final letter is psSP
	private static final Pattern finalLetter = Pattern.compile("[^psSP]$");
	// p s must be the first letter
	private static final Pattern ps = Pattern.compile(".[ps]");
	// c must be next to PS
	private static final Pattern nextC = Pattern.compile("[^PS]c|c[^PS]|[^PSps][PS]c|c[PS][^PS]");
	private static final Pattern nextK = Pattern.compile("[^PSKiIJ]K[^PSKiIJ]|[^psPS][PS]K|K[PS][^PS]");
	private static final Pattern nextJ = Pattern.compile("[^IJK]J|J[^IJK]");
	private static final Pattern smallI = Pattern.compile("[^Kd]i|i[^fK]|[^PSK]Ki|iK[^KPS]");
	private static final Pattern largeI = Pattern.compile("[^IJK]I|I[^IJK]");

	// phase turning R is above the CMB
	private static final Pattern mantleP = Pattern.compile("^P$|^P[PS]|[psPS]P$|[psPS]P[PS]");
	private static final Pattern mantleS = Pattern.compile("^S$|^S[PS]|[psPS]S$|[psPS]S[PS]");

	// diffraction phase
	private static final Pattern pDiff = Pattern.compile("Pdiff\\d*(\\.\\d+)?$");
	private static final Pattern sDiff = Pattern.compile("Sdiff\\d*(\\.?\\d+)?$");
	private static final Pattern diffRule = Pattern.compile("diff.+diff|P.*Pdiff|S.*Sdiff|diff.*[^\\d]$");

	// phase reflected at the cmb
	private static final Pattern cmbP = Pattern.compile("Pc|cP");
	private static final Pattern cmbS = Pattern.compile("Sc|cS");

	// phase turning R r <cmb
	private static final Pattern outercoreP = Pattern.compile("PK|KP");
	private static final Pattern outercoreS = Pattern.compile("SK|KS");

	// phase turning R icb < r < cmb, i.e., the phase does not go to inner core.
	private static final Pattern outercore = Pattern.compile("[PSK]K[PSK]");

	// frequently use
	public static final Phase P = Phase.create("P");
	public static final Phase PcP = Phase.create("PcP");
	public static final Phase PKiKP = Phase.create("PKiKP");
	public static final Phase PKIKP = Phase.create("PKIKP");
	public static final Phase S = Phase.create("S");
	public static final Phase ScS = Phase.create("ScS");
	public static final Phase SKS = Phase.create("SKS");
	public static final Phase SKiKS = Phase.create("SKiKS");
	public static final Phase SKIKS = Phase.create("SKIKS");

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compiledName == null) ? 0 : compiledName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Phase other = (Phase) obj;
		if (compiledName == null) {
			if (other.compiledName != null)
				return false;
		} else if (!compiledName.equals(other.compiledName))
			return false;
		return true;
	}

	/**
	 * (input) phase name e.g. (SmKS)
	 */
	private final String phaseName;

	/**
	 * name for parsing e.g. (SKKKKKS)
	 */
	private final String compiledName;

	private Phase(String phaseName, String compiledName) {
		this.phaseName = phaseName;
		this.compiledName = compiledName;
		computeParts();
		digitalize();
		setPropagation();
	}

	/**
	 * @param name
	 *            phase name
	 * @return phase for input
	 * @throws IllegalArgumentException
	 *             if the phase is invalid
	 */
	public static Phase create(String name) {
		String compiledName = compile(name);
		if (isValid(compiledName))
			return new Phase(name, compiledName);
		throw new IllegalArgumentException("Invalid phase name " + name);
	}

	private static String compile(String name) {
		try {
			Matcher m = number.matcher(name);
			int dPos = name.indexOf('d');
			while (m.find() && m.start() < dPos) {
				int n = Integer.parseInt(m.group());
				char c = name.charAt(m.end());
				char[] chars = new char[n - 1];
				Arrays.fill(chars, c);
				String rep = String.copyValueOf(chars);
				name = m.replaceFirst(rep);
				m = number.matcher(name);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid phase name " + name);
		}
		return name;
	}

	/**
	 * the number of Parts part is for each layer and each direction. if there
	 * is a turning point in one layer the number of parts will increase for
	 * example S wave has 2 parts.
	 * 
	 */
	private int nPart;

	/**
	 * if each part waveforms go deeper or shallower. if waveforms go deeper
	 * true, if else false.
	 */
	private boolean[] isDownGoing;

	/**
	 * if each part is P wave or S wave P:true, S:false
	 */
	private boolean[] isP;

	/**
	 * where waveform is at each part
	 */
	private Partition[] partition;

	/**
	 * @return angle of diffraction [rad]
	 */
	double getDiffractionAngle() {
		return diffractionAngle;
	}

	private void computeParts() {
		int reflection = 0;
		// waveform goes to a deeper part.
		int zoneMove = 0;
		for (int i = 0; i < compiledName.length(); i++)
			if (compiledName.charAt(i) == 'd')
				break;
			else if (compiledName.charAt(i) == 'c' || compiledName.charAt(i) == 'i')
				reflection++;
			else if (compiledName.charAt(i) == 'K'
					&& (compiledName.charAt(i - 1) == 'P' || compiledName.charAt(i - 1) == 'S'))
				zoneMove++;
			else if ((compiledName.charAt(i) == 'I' || compiledName.charAt(i) == 'J')
					&& compiledName.charAt(i - 1) == 'K')
				zoneMove++;
		// reflection++;
		int dPos = compiledName.indexOf('d');
		nPart = ((0 < dPos ? dPos : compiledName.length()) - reflection * 2 - zoneMove) * 2;
		if (compiledName.charAt(0) == 'p' || compiledName.charAt(0) == 's')
			nPart -= 1;

		// diffraction
		if (0 < dPos && dPos+4<compiledName.length())
			diffractionAngle = Math.toRadians(Double.parseDouble(compiledName.substring(dPos + 4)));

		isDownGoing = new boolean[nPart];
		isP = new boolean[nPart];
		partition = new Partition[nPart];
	}

	boolean isDiffracted() {
		return compiledName.contains("diff");
	}

	/**
	 * Angle of Diffraction
	 */
	private double diffractionAngle;

	private void digitalize() {
		// P
		int iCurrentPart = 0;
		for (int i = 0; i < compiledName.length(); i++) {
			switch (compiledName.charAt(i)) {
			case 'd':
				break;
			case 'c':
				continue;
			case 'i':
				continue;
			case 'p':
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 's':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 'P':
				isP[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.MANTLE;
				isDownGoing[iCurrentPart] = i == 0
						|| (compiledName.charAt(i - 1) != 'K' && compiledName.charAt(i - 1) != 'c');
				if (i == 0 || ((compiledName.charAt(i - 1) != 'c' && compiledName.charAt(i - 1) != 'K')))
					if ((i + 1) == compiledName.length()
							|| (compiledName.charAt(i + 1) != 'c' && compiledName.charAt(i + 1) != 'K')) {
						iCurrentPart++;
						isDownGoing[iCurrentPart] = false;
						isP[iCurrentPart] = true;
						partition[iCurrentPart] = Partition.MANTLE;
					}
				iCurrentPart++;
				break;
			case 'S':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = i == 0
						|| (compiledName.charAt(i - 1) != 'K' && compiledName.charAt(i - 1) != 'c');
				partition[iCurrentPart] = Partition.MANTLE;
				if (i == 0 || ((compiledName.charAt(i - 1) != 'c' && compiledName.charAt(i - 1) != 'K')))
					if ((i + 1) == compiledName.length()
							|| (compiledName.charAt(i + 1) != 'c' && compiledName.charAt(i + 1) != 'K')) {
						iCurrentPart++;
						partition[iCurrentPart] = Partition.MANTLE;
						isP[iCurrentPart] = false;
						isDownGoing[iCurrentPart] = false;
					}
				iCurrentPart++;
				break;
			case 'I':
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				break;
			case 'J':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				break;
			case 'K':
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = compiledName.charAt(i - 1) != 'i' && compiledName.charAt(i - 1) != 'I'
						&& compiledName.charAt(i - 1) != 'J';
				partition[iCurrentPart] = Partition.OUTERCORE;
				if (compiledName.charAt(i - 1) != 'i' && compiledName.charAt(i - 1) != 'I'
						&& compiledName.charAt(i - 1) != 'J')
					if (compiledName.charAt(i + 1) != 'i' && compiledName.charAt(i + 1) != 'I'
							&& compiledName.charAt(i + 1) != 'J') {
						iCurrentPart++;
						isDownGoing[iCurrentPart] = false;
						isP[iCurrentPart] = true;
						partition[iCurrentPart] = Partition.OUTERCORE;
					}
				iCurrentPart++;
				break;
			}
		}

	}

	private void setPropagation() {
		if (compiledName.contains("P"))
			mantlePPropagation = Propagation.BOUNCING;
		if (compiledName.contains("PK") || compiledName.contains("Pc") || compiledName.contains("KP")
				|| compiledName.contains("cP"))
			mantlePPropagation = Propagation.PENETRATING;
		if(compiledName.contains("Pdiff"))
			mantlePPropagation = Propagation.DIFFRACTION;
		
		if (compiledName.contains("S"))
			mantleSPropagation = Propagation.BOUNCING;
		if (compiledName.contains("SK") || compiledName.contains("Sc") || compiledName.contains("KS")
				|| compiledName.contains("cS"))
			mantleSPropagation = Propagation.PENETRATING;
		if( compiledName.contains("Sdiff"))
			mantleSPropagation=Propagation.DIFFRACTION;
		
		if (compiledName.contains("I") || compiledName.contains("J") || compiledName.contains("i"))
			kPropagation = Propagation.PENETRATING;
		else if (compiledName.contains("K"))
			kPropagation = Propagation.BOUNCING;

		if (compiledName.contains("I"))
			innerCorePPropagation = Propagation.BOUNCING;

		if (compiledName.contains("J"))
			innerCoreSPropagation = Propagation.BOUNCING;

	}

	public static void main(String[] args) {
		System.out.println(Phase.create("2PSdiff900"));
		// System.out.println(isValid("PKPScPSdiff10000"));
	}

	/**
	 * @param phase
	 *            phase name
	 * @return if is well known phase?
	 */
	static boolean isValid(String phase) {
		if (phase.equals(""))
			return false;
		// diffraction phase
		if (diffRule.matcher(phase).find())
			return false;
		if (!pDiff.matcher(phase).find() && !sDiff.matcher(phase).find() && finalLetter.matcher(phase).find())
			return false;

		// check if other letters are used.
		if (others.matcher(phase).find())
			return false;

		if (firstLetter.matcher(phase).find())
			return false;

		if (ps.matcher(phase).find())
			return false;

		if (nextC.matcher(phase).find())
			return false;

		if (nextK.matcher(phase).find())
			return false;

		if (nextJ.matcher(phase).find())
			return false;

		if (smallI.matcher(phase).find())
			return false;

		if (largeI.matcher(phase).find())
			return false;

		// phase reflected at the icb
		boolean icb = phase.contains("Ki") || phase.contains("iK");
		boolean innercoreP = phase.contains("I");
		boolean innercoreS = phase.contains("J");

		if (mantleP.matcher(phase).find())
			if (cmbP.matcher(phase).find() || outercoreP.matcher(phase).find() || innercoreP)
				return false;

		if (mantleS.matcher(phase).find())
			if (cmbS.matcher(phase).find() || outercoreS.matcher(phase).find() || innercoreS)
				return false;

		if (outercore.matcher(phase).find())
			if (innercoreP || icb || innercoreS)
				return false;
		return true;
	}

	private Propagation mantlePPropagation;
	private Propagation innerCorePPropagation;
	private Propagation kPropagation;

	private Propagation mantleSPropagation;
	private Propagation innerCoreSPropagation;

	Propagation getMantlePPropagation() {
		return mantlePPropagation;
	}

	Propagation getInnerCorePPropagation() {
		return innerCorePPropagation;
	}

	Propagation getkPropagation() {
		return kPropagation;
	}

	Propagation getMantleSPropagation() {
		return mantleSPropagation;
	}

	Propagation getInnerCoreSPropagation() {
		return innerCoreSPropagation;
	}

	/**
	 * @param pTurning
	 *            turning point of P
	 * @param sTurning
	 *            turning point of S
	 * @return if the input turning points are valid for this
	 */
	boolean exists(Raypath ray) {
		if (mantlePPropagation != null && ray.getMantlePPropagation() != mantlePPropagation)
			return false;
		if (kPropagation != null && ray.getkPropagation() != kPropagation)
			return false;
		if (innerCorePPropagation != null && ray.getInnerCorePPropagation() != innerCorePPropagation)
			return false;
		if (ray.isSv()) {
			if (mantleSPropagation != null && ray.getMantleSVPropagation() != mantleSPropagation)
				return false;
			if (innerCoreSPropagation != null && ray.getInnerCoreSVPropagation() != innerCoreSPropagation)
				return false;
		} else {
			if (mantleSPropagation != null && ray.getMantleSHPropagation() != mantleSPropagation)
				return false;
			if (innerCoreSPropagation != null && ray.getInnerCoreSHPropagation() != innerCoreSPropagation)
				return false;
		}
		return true;
	}

	/**
	 * deepest part of P phase
	 * 
	 * @return null if P phase does not exist.
	 */
	Partition pReaches() {
		if (compiledName.contains("Sdiff"))
			return null;
		if (compiledName.contains("I"))
			return Partition.INNERCORE;
		if (compiledName.contains("i") || compiledName.contains("J"))
			return Partition.INNER_CORE_BAUNDARY;
		if (compiledName.contains("K"))
			return Partition.OUTERCORE;
		if (!compiledName.contains("P") && !compiledName.contains("p"))
			return null;
		if (compiledName.contains("Pc") || compiledName.contains("cP") || compiledName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;
		return Partition.MANTLE;
	}

	/**
	 * deepest part of s Phase
	 * 
	 * @return null if S phase does not exist.
	 */
	Partition sReaches() {
		if (compiledName.contains("J"))
			return Partition.INNERCORE;
		if (!compiledName.contains("S"))
			return null;
		if (compiledName.contains("Sc") || compiledName.contains("cS") || compiledName.contains("KS")
				|| compiledName.contains("SK") || compiledName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;
		return Partition.MANTLE;
	}

	/**
	 * @return how many times P wave travels in the mantle. each down or upgoing
	 *         is 0.5
	 */
	double mantleP() {
		double pNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.MANTLE) && isP[i])
				pNum += 0.5;
		return compiledName.charAt(0) == 'p' ? pNum - 0.5 : pNum;
	}

	/**
	 * {@link #nPart}
	 * 
	 * @return the number of part
	 */
	int getNPart() {
		return nPart;
	}

	/**
	 * @param i
	 *            index of the target part
	 * @return if it is p phase in i th part.
	 */
	boolean partIsP(int i) {
		return isP[i];
	}

	/**
	 * @param i
	 *            index of the target
	 * @return if it is down going in i th part.
	 */
	boolean partIsDownGoing(int i) {
		return isDownGoing[i];
	}

	/**
	 * @param i
	 *            index of the target
	 * @return the partition in which i th part is
	 */
	Partition partIs(int i) {
		return partition[i];
	}

	/**
	 * how many times S wave travels in the mantle. each down or upgoing is 0.5
	 * 
	 * @return
	 */
	double mantleS() {
		double sNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.MANTLE) && !isP[i])
				sNum += 0.5;

		return compiledName.charAt(0) == 's' ? sNum - 0.5 : sNum;
	}

	/**
	 * how many times wave travels in the outer core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times K phase travels in the outer core.
	 */
	double outerCore() {
		double outerCoreNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.OUTERCORE))
				outerCoreNum += 0.5;

		return outerCoreNum;
	}

	/**
	 * how many times P wave travels in the inner core. Each down or upgoing is
	 * considered as 0.5
	 * 
	 * @return the times P wave travels in the inner core
	 */
	double innerCoreP() {
		return compiledName.chars().filter(c -> c == 'I').count();
	}

	/**
	 * how many times S wave travels in the inner core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times S wave travels in the inner core.
	 */
	double innerCoreS() {
		return compiledName.chars().filter(c -> c == 'J').count();
	}

	@Override
	public String toString() {
		return phaseName;
	}
}