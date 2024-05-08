/*
 * Copyright (c) 2014 University of Tartu
 */
package org.qsardb.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.dmg.pmml.DataField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.qsardb.model.Model;
import org.qsardb.model.Prediction;

public class ClassificationStatistics implements Statistics {
	private final int size;
	private final List<String> categories;
	private double accuracy;
	private int[][] confusion;
	private double[] sensitivities;
	private double[] specificities;

	public ClassificationStatistics(Model model, Prediction prediction) {
		if (StatisticsUtil.isRegression(model)) {
			throw new IllegalArgumentException("Expected classification model: "+model.getId());
		}

		Map<String, String> predicted = StatisticsUtil.loadValues(prediction);
		size = predicted.size();

		Map<String, String> actual = StatisticsUtil.loadValues(model.getProperty());
		HashSet<String> clist = new HashSet<String>(actual.values());
		clist.remove(null); // remove missing value from category names
		categories = Collections.unmodifiableList(new ArrayList<String>(clist));

		makeConfusionMatrix(actual, predicted);
		initializeStats();
	}

	@Override
	public int size() {
		return size;
	}

	public double accuracy() {
		return accuracy;
	}

	public List<String> categories() {
		return categories;
	}

	public int confusionMatrix(int rowActual, int colPredicted) {
		return confusion[rowActual][colPredicted];
	}

	public double sensitivity(int i) {
		return sensitivities[i];
	}

	public double specificity(int i) {
		return specificities[i];
	}

	private void makeConfusionMatrix(Map<String, String> actual, Map<String, String> predicted) {
		confusion = new int[categories.size()][categories.size()];
		for (String k: predicted.keySet()) {
			for (int i=0; i<categories.size(); ++i) {
				String ci = categories.get(i);
				for (int j=0; j<categories.size(); ++j) {
					String cj = categories.get(j);
					if (ci.equals(actual.get(k)) && cj.equals(predicted.get(k))) {
						confusion[i][j]++;
					}
				}
			}
		}
	}

	private void initializeStats() {
		accuracy = 0.0;
		sensitivities = new double[categories.size()];
		specificities = new double[categories.size()];

		int total = 0;
		int[] actualCounts = new int[categories.size()];
		int[] predictedCounts = new int[categories.size()];
		for (int i=0; i<categories.size(); ++i) {
			actualCounts[i] = 0;
			predictedCounts[i] = 0;
			for (int j=0; j<categories.size(); ++j) {
				actualCounts[i] += confusion[i][j];
				predictedCounts[i] += confusion[j][i];
			}
			total += actualCounts[i];
		}

		// test set has no experimental data
		if (total == 0) {
			accuracy = Double.NaN;
			Arrays.fill(sensitivities, Double.NaN);
			Arrays.fill(specificities, Double.NaN);
			return;
		}

		for (int i=0; i<categories.size(); ++i) {
			accuracy += confusionMatrix(i, i);
		}
		accuracy /= total; 

		for (int i=0; i<categories.size(); ++i) {
			sensitivities[i] = confusion[i][i] / (double)actualCounts[i];
			specificities[i] = 0;
			for (int k=0; k<categories.size(); ++k) {
				if (k != i) {
					specificities[i] += predictedCounts[k] - confusion[i][k];
				}
			}
			specificities[i] /= total - actualCounts[i];
		}
	}
}
