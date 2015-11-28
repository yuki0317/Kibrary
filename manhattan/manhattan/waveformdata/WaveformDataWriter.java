package manhattan.waveformdata;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import filehandling.sac.WaveformType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.HorizontalPosition;
import manhattan.template.Location;
import manhattan.template.Station;

/**
 * BasicDatasetやPartialDatasetの書き込み
 * 
 * This class create a new set of dataset files.
 * 
 * @since 2013/11/13
 * 
 * 
 * @version 0.4
 * 
 * @author kensuke
 * 
 */

/**
 * @author kensuke 
 * @since 2015/11/24 
 * @version 0.0.1
 */
/**
 * @author kensuke
 * @since 2015/11/24
 * @version 0.0.1
 */
public class WaveformDataWriter implements Closeable, Flushable {
	public Path getIDPath() {
		return idPath;
	}

	public Path getDataPath() {
		return dataPath;
	}

	/**
	 * id情報の書き出し
	 */
	private DataOutputStream idStream;

	/**
	 * 波形情報の書き出し
	 */
	private DataOutputStream dataStream;

	/**
	 * id information file
	 */
	private Path idPath;

	/**
	 * 波形情報ファイル
	 */
	private Path dataPath;

	@Override
	public void close() throws IOException {
		idStream.close();
		dataStream.close();
	}

	@Override
	public void flush() throws IOException {
		idStream.flush();
		dataStream.flush();
	}

	/**
	 * Because the header part is decided when this is constructed, the mode is
	 * also decided(0: BasicID, 1: PartialID)
	 */
	private final int mode;

	/**
	 * index map for stations
	 */
	private Map<Station, Integer> stationMap;
	/**
	 * index map for global CMT IDs
	 */
	private Map<GlobalCMTID, Integer> globalCMTIDMap;

	/**
	 * index map for perturbation location
	 */
	private Map<Location, Integer> perturbationLocationMap;
	/**
	 * index for period ranges
	 */
	private double[][] periodRanges;

	/**
	 * This constructor is only for BasicID. All output ID must have a station,
	 * a Global CMT ID and period ranges in the input ones.
	 * 
	 * @param idPath
	 *            Path for ID file (must not exist)
	 * @param dataPath
	 *            Path for data file (must not exist)
	 * @param stationSet
	 *            must contain all information of the IDs to output
	 * @param globalCMTIDSet
	 *            must contain all information of the IDs to output
	 * @param periodRanges
	 *            must contain all information of the IDs to output
	 * @throws IOException
	 *             if an error occurs
	 */
	public WaveformDataWriter(Path idPath, Path dataPath, Set<Station> stationSet, Set<GlobalCMTID> globalCMTIDSet,
			double[][] periodRanges) throws IOException {
		this(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, null);
	}

	/**
	 * This constructor is only for PartialID. All output ID must have a
	 * station, a Global CMT ID and period ranges in the input ones.
	 * 
	 * @param idPath
	 *            Path for ID file (must not exist)
	 * @param dataPath
	 *            Path for data file (must not exist)
	 * @param stationSet
	 *            must contain all information of the IDs to output
	 * @param globalCMTIDSet
	 *            must contain all information of the IDs to output
	 * @param periodRanges
	 *            must contain all information of the IDs to output
	 * @param perturbationPoints
	 *            must contain all information of the IDs to output
	 * @throws IOException
	 *             if an error occurs
	 */
	public WaveformDataWriter(Path idPath, Path dataPath, Set<Station> stationSet, Set<GlobalCMTID> globalCMTIDSet,
			double[][] periodRanges, Set<Location> perturbationPoints) throws IOException {
		this.idPath = idPath;
		this.dataPath = dataPath;
		if (checkDuplication(periodRanges))
			throw new RuntimeException("Input periodRanges have duplication.");
		this.periodRanges = periodRanges;
		idStream = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(idPath, StandardOpenOption.CREATE_NEW)));
		dataStream = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(dataPath, StandardOpenOption.CREATE_NEW)));
		dataLength = Files.size(dataPath);
		idStream.writeShort(stationSet.size());
		idStream.writeShort(globalCMTIDSet.size());
		idStream.writeShort(periodRanges.length);
		if (perturbationPoints != null)
			idStream.writeShort(perturbationPoints.size());
		makeStationMap(stationSet);
		makeGlobalCMTIDMap(globalCMTIDSet);
		for (int i = 0; i < periodRanges.length; i++) {
			idStream.writeFloat((float) periodRanges[i][0]);
			idStream.writeFloat((float) periodRanges[i][1]);
		}
		if (perturbationPoints != null)
			makePerturbationMap(perturbationPoints);
		mode = perturbationPoints == null ? 0 : 1;
	}

	private void makeGlobalCMTIDMap(Set<GlobalCMTID> globalCMTIDSet) throws IOException {
		int i = 0;
		globalCMTIDMap = new HashMap<>();
		for (GlobalCMTID id : globalCMTIDSet) {
			globalCMTIDMap.put(id, i++);
			idStream.writeBytes(StringUtils.rightPad(id.toString(), 15));
		}
	}

	private void makePerturbationMap(Set<Location> perturbationMap) throws IOException {
		int i = 0;
		perturbationLocationMap = new HashMap<>();
		for (Location loc : perturbationMap) {
			perturbationLocationMap.put(loc, i++);
			idStream.writeFloat((float) loc.getLatitude());
			idStream.writeFloat((float) loc.getLongitude());
			idStream.writeFloat((float) loc.getR());
		}
	}

	private void makeStationMap(Set<Station> stationSet) throws IOException {
		int i = 0;
		stationMap = new HashMap<>();
		for (Station station : stationSet) {
			stationMap.put(station, i++);
			idStream.writeBytes(StringUtils.rightPad(station.getStationName(), 8));
			idStream.writeBytes(StringUtils.rightPad(station.getNetwork(), 8));
			HorizontalPosition pos = station.getPosition();
			idStream.writeFloat((float) pos.getLatitude());
			idStream.writeFloat((float) pos.getLongitude());
		}
	}

	private static boolean checkDuplication(double[][] periodRanges) {
		for (int i = 0; i < periodRanges.length - 1; i++)
			for (int j = i + 1; j < periodRanges.length; j++)
				if (Arrays.equals(periodRanges[i], periodRanges[j]))
					return true;
		return false;
	}

	/**
	 * The file size (byte). (should be StartByte)
	 */
	private long dataLength;

	/**
	 * 波形データを書き込む
	 * 
	 * @param data
	 */
	private void addWaveform(double[] data) throws IOException {
		for (int i = 0; i < data.length; i++)
			dataStream.writeDouble(data[i]);
		dataLength += 8 * data.length;
	}

	/**
	 * @param basicID
	 *            StartByte will be ignored and set properly in the output file.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public synchronized void addBasicID(BasicID basicID) throws IOException {
		if (mode != 0)
			throw new RuntimeException("No BasicID please, would you.");

		switch (basicID.waveFormType) {
		case OBS:
			idStream.writeBoolean(true); // if it is obs 1Byte
			break;
		case SYN:
			idStream.writeBoolean(false); // if it is obs
			break;
		default:
			throw new RuntimeException("This is a partial derivative.");
		}
		long startByte = dataLength;
		addWaveform(basicID.getData());
		idStream.writeShort(stationMap.get(basicID.station));
		idStream.writeShort(globalCMTIDMap.get(basicID.globalCMTID));
		idStream.writeByte(basicID.sacComponent.valueOf());
		idStream.writeByte(getIndexOfRange(basicID.minPeriod, basicID.maxPeriod));

		// 4Byte
		idStream.writeFloat((float) basicID.getStartTime()); // start time 4Byte
		idStream.writeInt(basicID.getNpts()); // データポイント数 4Byte
		idStream.writeFloat((float) basicID.getSamplingHz()); // sampling Hz 4
		// Byte
		// convolutionされているか 観測波形なら true
		idStream.writeBoolean(basicID.getWaveformType() == WaveformType.OBS || basicID.isConvolved); // 1Byte
		idStream.writeLong(startByte); // データの格納場所 8 Byte

	}

	private int getIndexOfRange(double min, double max) {
		for (int i = 0; i < periodRanges.length; i++) // TODO
			if (Math.abs(periodRanges[i][0] - min) < 0.000000001 && Math.abs(periodRanges[i][1] - max) < 0.000000001)
				return i;
		throw new RuntimeException("A range is N/A");
	}

	/**
	 * @param partialID
	 *            {@link PartialID} must contain waveform data. StartByte will
	 *            be ignored and set properly in the output file.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public synchronized void addPartialID(PartialID partialID) throws IOException {
		if (partialID.waveFormType != WaveformType.PARTIAL)
			throw new RuntimeException(
					"This is not a partial derivative. " + Thread.currentThread().getStackTrace()[1].getMethodName());
		if (mode != 1)
			throw new RuntimeException("No Partial please, would you.");
		long startByte = dataLength;
		addWaveform(partialID.getData());
		idStream.writeShort(stationMap.get(partialID.station));
		idStream.writeShort(globalCMTIDMap.get(partialID.globalCMTID));
		idStream.writeByte(partialID.sacComponent.valueOf());
		idStream.writeByte(getIndexOfRange(partialID.minPeriod, partialID.maxPeriod));
		idStream.writeFloat((float) partialID.startTime); // start time 4 Byte
		idStream.writeInt(partialID.npts); // データポイント数 4 Byte
		idStream.writeFloat((float) partialID.samplingHz); // sampling Hz 4 Byte
		// convolutionされているか
		idStream.writeBoolean(partialID.isConvolved); // 1Byte
		idStream.writeLong(startByte); // データの格納場所 8 Byte
		// partial type 1 Byte
		idStream.writeByte(partialID.getPartialType().getValue());
		idStream.writeShort(perturbationLocationMap.get(partialID.pointLocation));
	}

}