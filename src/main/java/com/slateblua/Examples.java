package com.slateblua;

import com.slateblua.dice.DiceSimulation;
import com.slateblua.montecarlo.KineticMonteCarlo;
import com.slateblua.simulation.KmcSimulationResult;

import java.util.Locale;
import java.util.Random;

public class Examples {
    public static void main(String[] args) {
        // Dice board game Markov chain (Homework 1)
        DiceSimulation diceSimulation = new DiceSimulation();
        diceSimulation.solve();

        // Kinetic Monte Carlo driven by physical time (Homework 2)
        KineticMonteCarlo kmc = new KineticMonteCarlo(new Random());

        double duration = 1_000.0;
        KmcSimulationResult kmcResult = kmc.executeUntilTime(0L, duration);
        double density = kmcResult.finalState().sum() / kmc.sites();

        System.out.printf(
                Locale.ROOT,
                "KMC at t=%.2f: steps=%d, accepted=%d, finalDensity=%.4f%n",
                kmcResult.elapsedTime(),
                kmcResult.totalSteps(),
                kmcResult.acceptedSteps(),
                density
        );
    }
}
