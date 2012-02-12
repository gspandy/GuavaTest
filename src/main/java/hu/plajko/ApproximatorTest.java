package hu.plajko;

import hu.plajko.Approximator.Function;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApproximatorTest {

	private static final Logger log = LoggerFactory.getLogger(ApproximatorTest.class);
	private static final NumberFormat nf = new DecimalFormat("0.0####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	public static void main(String[] args) {
		/** Sample equation ***************************************************************************/
		Function f1 = new Function() {
			public double calculate(double a, double b, double c) {
				return a * a + b * b + c * c;
			}
		};
		double solution = new Approximator(f1, 127.0d).findSolution(null, 3.0d, 2.0d);
		log.info("{}", nf.format(solution));
		log.info("{}", nf.format(f1.calculate(solution, 3.0d, 2.0d)));


		/** Annuity ***************************************************************************/
		Function annuity = new Function() {
			public double calculate(double a, double r, double exp) {
				return a * (1 - (1 / (Math.pow(1 + r, exp)))) / r;
			}
		};
		double b = 1000.0d;
		Approximator approximator = new Approximator(annuity, b);

		double a = 100.0d;
		double exp = 25.0d;

		double solution_r = approximator.findSolution(a, null, exp);
		log.info("r={}", nf.format(solution_r));
		log.info("(check) {} = {}", nf.format(b), nf.format(annuity.calculate(a, solution_r, exp)));

		double solution_a = approximator.findSolution(null, solution_r, exp);
		log.info("a={}", nf.format(solution_a));
		log.info("(check) {} = {}", nf.format(b), nf.format(annuity.calculate(solution_a, solution_r, exp)));

		double solution_exp = approximator.findSolution(solution_a, solution_r, null);
		log.info("exp={}", nf.format(solution_exp));
		log.info("(check) {} = {}", nf.format(b), nf.format(annuity.calculate(solution_a, solution_r, solution_exp)));
	}

}
