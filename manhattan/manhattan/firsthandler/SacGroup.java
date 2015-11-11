package manhattan.firsthandler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import filehandling.sac.SACHeaderEnum;
import filehandling.sac.SACUtil;
import manhattan.template.Utilities;

/**
 * 
 * rdseedからできた 1993.052.07.01.12.4000.PS.OGS.(locationID).BHN.D.SAC
 * の用なファイルのうち、同じnetwork, station, locationID, channel, qualityID のもののgroup
 * 
 * @since 2013/9/22
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2014/1/13 calendarをつかうようにした
 * 
 * @version 0.0.3
 * @since 2014/4/29 {@link #merge()}において足し合わせた時の時間差が小さい時は表示しないようにした
 * 
 * @version 0.0.5
 * @since 2015/2/12 {@link Calendar} &rarr; {@link LocalDateTime}
 * 
 * @version 0.0.5.1
 * @since 2015/8/5 {@link #merge()} throws {@link IOException}
 * 
 * @version 0.0.6
 * @since 2015/8/19 {@link Path} base
 * 
 * @author kensuke
 * 
 */
class SacGroup {

	/**
	 * mergeする際のファイルのタイムウインドウのずれの許容範囲 sacfile のDELTA * maxgapNumber
	 * 以上タイムウインドウに開きがあるとmergeしない
	 */
	private static final int maxGapNumber = 500;

	/**
	 * 作業フォルダ
	 */
	private Path workPath;

	private Set<SacFileName> nameSet = new HashSet<>();

	/**
	 * mergeしたSacFileName
	 */
	private String mergedSacFileName;

	/**
	 * 基準となる {@link SacFileName}
	 */
	private SacFileName rootSacFileName;

	/**
	 * 基本となる {@link SacFileName}を追加
	 * 
	 * @param workPath
	 * @param sacFileName
	 */
	SacGroup(Path workPath, SacFileName sacFileName) {
		this.workPath = workPath;
		nameSet.add(sacFileName);
		rootSacFileName = sacFileName;
		mergedSacFileName = sacFileName.getRelationString();
	}

	/**
	 * SacSetに{@link SacFileName}を加える 既に同じものが入っていたり
	 * {@link SacFileName#isRelated(SacFileName)}がfalseの場合追加しない
	 * 
	 * @param sacFileName
	 * @return 追加したかどうか
	 */
	boolean add(SacFileName sacFileName) {
		return rootSacFileName.isRelated(sacFileName) && nameSet.add(sacFileName);
	}

	/**
	 * グループ内のSAC fileを trashに捨てる 存在していないと作成する
	 * 
	 * @return 捨てられたかどうか
	 */
	void move(Path trash) {
		nameSet.stream().map(name -> name.toString()).map(workPath::resolve).forEach(srcPath -> {
			try {
				Utilities.moveToDirectory(srcPath, trash, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * mergeしていく。 sortした後のファイルを一つ一つつなげていく だめな条件が出たファイルは関連するファイルも含めてゴミ箱行き
	 * name1にname2をくっつける。 name1とname2が離れすぎていると そこでだめになる その一連のファイルもだめ
	 * 
	 * @return うまくつなげられたかどうか
	 */
	boolean merge() throws IOException {
		// System.out.println("merging");
		SacFileName[] sacFileNameList = nameSet.toArray(new SacFileName[nameSet.size()]);
		// sort the sacFileNameList
		Arrays.sort(sacFileNameList);

		// 基準となるSacを読み込む
		Path standardSacPath = workPath.resolve(sacFileNameList[0].toString());
		Map<SACHeaderEnum, String> headerMap = SACUtil.readHeader(standardSacPath);
		double[] standardSacData = SACUtil.readSACData(standardSacPath);
		final double delta = Double.parseDouble(headerMap.get(SACHeaderEnum.DELTA));
		final long deltaInMillis = Math.round(1000 * delta);
		// currentEndTimeとスタート時刻がmaxGap(msec) を超える波形はくっつけられない
		long maxGap = deltaInMillis * maxGapNumber;
		// half length of delta0 (msec)
		long halfDelta = deltaInMillis / 2;
		int currentNpts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
		// つなげていく波形
		List<Double> sacdata = new ArrayList<>(currentNpts);

		// timewindow length (msec)
		long timelength = deltaInMillis * (currentNpts - 1);
		double e0 = Double.parseDouble(headerMap.get(SACHeaderEnum.E));
		// System.out.println(e0+" "+headerMap.get(SacHeaderEnum.E));
		double currentB = Double.parseDouble(headerMap.get(SACHeaderEnum.B));
		// b value (msec)
		final long bInMillis = Math.round(currentB * 1000);
		// System.out.println(currentB*1000+" "+e0);
		// e value (msec)
		long eInMillis = Math.round(e0 * 1000);
		// System.out.println("b, e "+bInMillis+", "+eInMillis);
		// current start time of waveform
		LocalDateTime currentStartTime = rootSacFileName.getStartTime().plus(bInMillis, ChronoUnit.MILLIS);
		// current end time of waveform startTime+ timelength0
		LocalDateTime currentEndTime = currentStartTime.plus(timelength, ChronoUnit.MILLIS);

		for (int j = 0; j < currentNpts; j++)
			sacdata.add(standardSacData[j]);

		for (int i = 1; i < sacFileNameList.length; i++) {
			// sacfilename to be joined
			SacFileName joinSacFileName = sacFileNameList[i];
			Path joinSacPath = workPath.resolve(joinSacFileName.toString());
			// System.out.println("joining " + joinSacFile);

			// つなげるsacfileの読み込み
			Map<SACHeaderEnum, String> headerMap1 = SACUtil.readHeader(joinSacPath);
			int npts = Integer.parseInt(headerMap1.get(SACHeaderEnum.NPTS));
			// double e = Double.parseDouble(headerMap1.get(SacHeaderEnum.E));
			double b = Double.parseDouble(headerMap1.get(SACHeaderEnum.B));
			long joinBInMillis = Math.round(b * 1000);
			// start time for joinSacfile
			LocalDateTime startTime = joinSacFileName.getStartTime().plus(joinBInMillis, ChronoUnit.MILLIS);
			// time length of joinsacfile (msec)
			timelength = deltaInMillis * (npts - 1);
			// end time of joinSacFile
			LocalDateTime endTime = startTime.plus(timelength, ChronoUnit.MILLIS);

			// 終了時刻がcurrentEndTimeより早いとmergeしない TODO 例外あり得るか？
			// 開始時刻はファイル名で並べられているはず
			if (endTime.isBefore(currentEndTime))
				continue;

			// 直前のsac終了時刻から本sacの開始時刻までの時間差（ミリ秒）
			// 正なら重複部分なし 負なら重複時間あり
			long timeGap = currentEndTime.until(startTime, ChronoUnit.MILLIS);

			// 時間差がmaxGapより大きい場合NG TODO 将来的に0補完後捨てる？
			if (maxGap < timeGap)
				return false;

			// System.out.println("yes merging");
			double[] data = SACUtil.readSACData(joinSacPath);

			// 時間差が直前のファイルの終了時刻からDELTAの半分より後、それ以外で場合分け
			// 半分より後の場合はjoinsacの一番端をcurrentsacのとなりにくっつける
			// 半分より前の場合は調整してくっつける
			if (halfDelta < timeGap) {
				// joinsacがhalfDeltaより後に始まっている場合 そのままくっつける
				for (int j = 0; j < npts; j++)
					sacdata.add(data[j]);
				// currentE += npts * delta;
				eInMillis += npts * deltaInMillis;
				currentNpts += npts;
			} else {
				// 小さい場合
				// int gap = (int) (1.5 * delta0 * 1000 - timeGap) / 1000;
				// currentsacの終了時刻のdelta後から joinsacのstartの時間差(msec)
				long gap = deltaInMillis - timeGap;
				// joinsacのstartから何msec目がcurrentsacの隣の値になるか
				int gapI = (int) Math.round(gap / deltaInMillis);
				for (int j = gapI; j < npts; j++)
					sacdata.add(data[j]);
				// e0の更新
				// currentE += delta * (npts - gapI);
				eInMillis += (npts - gapI) * deltaInMillis;
				currentNpts += npts - gapI;
			}
			currentEndTime = endTime;
			currentStartTime = startTime;

		}
		// System.out.println(mergedSacFileName);

		if (sacdata.size() != currentNpts) {
			System.out.print("unexpected happened npts' are different ");
			System.out.println(sacdata.size() + " " + currentNpts);
			return false;
		}

		// System.out.println(deltaInMillis+" "+bInMillis+" "+eInMillis);
		long timeDiff = (sacdata.size() - 1) * deltaInMillis + bInMillis - eInMillis;
		if (5 < timeDiff || timeDiff < -5) {
			// if ((sacdata.size()-1)*deltaInMillis+bInMillis != eInMillis) {
			System.out.print("unexpected happened currentE' are different ");
			System.out.println((sacdata.size() - 1) * deltaInMillis + bInMillis + " " + eInMillis);
			if (100 < timeDiff || timeDiff < -100)
				return false;
		}
		double e = eInMillis / 1000.0;
		// System.out.println(e+" "+eInMillis);
		headerMap.put(SACHeaderEnum.NPTS, Integer.toString(sacdata.size()));
		headerMap.put(SACHeaderEnum.E, Double.toString(e));
		double[] sdata = sacdata.stream().mapToDouble(d -> d.doubleValue()).toArray();

		// System.exit(0);
		SACUtil.writeSAC(workPath.resolve(mergedSacFileName), headerMap, sdata);
		return true;

	}

}