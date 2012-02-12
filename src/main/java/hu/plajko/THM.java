package hu.plajko;

import hu.plajko.Approximator.Function;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class THM {

	private static final Logger log = LoggerFactory.getLogger(ApproximatorTest.class);
	private static final NumberFormat nf = new DecimalFormat("0.0####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	private static Function THMFunc = new Function() {
		public double calculate(double Ak, double THM, double periods) {
			double q = Math.pow(1.0d / (1.0d + THM / 100.0d), 1.0d / 12.0d);
			return Ak * q * (Math.pow(q, periods) - 1.0d) / (q - 1.0d);
		}
	};

	public static void main(String[] args) {
		lakashitel();
		aruhitel();
	}

	private static void lakashitel() {
		double amount = 10000000.0d;
		Approximator THMCalculator = new Approximator(THMFunc, amount);

		// Futamidő (hónap)
		double periods = 240.0d;

		// THM számolás
		for (double Ak = 92000.0d; Ak <= 114000d; Ak += 2000.0d) {
			double calculatedTHM = THMCalculator.findSolution(Ak, null, periods);
			log.info("Ak: {} - THM: {}", nf.format(Ak), nf.format(calculatedTHM));
		}

		// Törlesztőrészlet számolás
		for (double THM = 10.8d; THM <= 20.0d; THM += 0.2d) {
			double calculatedAk = THMCalculator.findSolution(null, THM, periods);
			log.info("Ak: {} - THM: {}", nf.format(calculatedAk), nf.format(THM));
		}
	}

	private static void aruhitel() {
		double amount = 800000.0d;
		Approximator THMCalculator = new Approximator(THMFunc, amount);

		// Futamidő (hónap)
		double periods = 12.0d;

		// THM számolás
		for (double Ak = 79000.0d; Ak <= 90000d; Ak += 500.0d) {
			double calculatedTHM = THMCalculator.findSolution(Ak, null, periods);
			log.info("Ak: {} - THM: {}", nf.format(Ak), nf.format(calculatedTHM));
		}

		// Törlesztőrészlet számolás
		for (double THM = 40.0d; THM <= 45.0d; THM += 0.05d) {
			double calculatedAk = THMCalculator.findSolution(null, THM, periods);
			log.info("Ak: {} - THM: {}", nf.format(calculatedAk), nf.format(THM));
		}
	}

}
