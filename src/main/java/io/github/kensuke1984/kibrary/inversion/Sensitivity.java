package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.RealMatrix;

public class Sensitivity {
	
	public static void main(String[] args) throws IOException {
//		Path partialIDPath = Paths.get("partialID.dat");
//		Path partialPath = Paths.get("partial.dat");
//		Path waveformIDPath = Paths.get(uri)
//		PartialID[] ids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
	}
	
	public static Map<Phases, Double> sensitivityPerWindowType(PartialID[] ids) {
		Map<Phases, Double> typeSensitivityMap = new HashMap<>();
		for (PartialID id : ids) {
			double s = Sensitivity1D.idToSensitivity(id);
			Phases phases = new Phases(id.getPhases());
			if (typeSensitivityMap.containsKey(phases)) {
				s += typeSensitivityMap.get(phases);
				typeSensitivityMap.replace(phases, s);
			}
			else
				typeSensitivityMap.put(phases, s);
		}
		return typeSensitivityMap;
	}
	
	public static Map<UnknownParameter, Double> sensitivityMap(RealMatrix ata, List<UnknownParameter> parameterList) {
		Map<UnknownParameter, Double> sensitivity = new HashMap<>();
		for (int i = 0; i < parameterList.size(); i++)
			sensitivity.put(parameterList.get(i), Math.sqrt(ata.getEntry(i, i)) * parameterList.get(i).getWeighting());
		return sensitivity;
	}
}