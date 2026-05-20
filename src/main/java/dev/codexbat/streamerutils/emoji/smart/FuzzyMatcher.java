// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.smart;

public final class FuzzyMatcher {
    private FuzzyMatcher() {}

    public static int score(String input, String target) {
        int score = 0;

        // exact
        if (target.equals(input)) return 1000;

        // strong prefix match (VERY important)
        if (target.startsWith(input)) {
            score += 300;

            // shorter target = better
            score += 50 - Math.min(target.length(), 50);
        }

        // subsequence match (s o b → sob)
        if (isSubsequence(input, target)) {
            score += 150;
        }

        // contains
        if (target.contains(input)) {
            score += 100;
        }

        // levenshtein penalty
        int distance = levenshtein(input, target);
        score -= distance * 10;

        // short input boost (this fixes :so:)
        if (input.length() <= 3) {
            score += 100;
        }

        return score;
    }

    private static boolean isSubsequence(String input, String target) {
        int i = 0, j = 0;
        while (i < input.length() && j < target.length()) {
            if (input.charAt(i) == target.charAt(j)) i++;
            j++;
        }
        return i == input.length();
    }

    private static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];

        for (int j = 0; j < costs.length; j++)
            costs[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;

            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }

        return costs[b.length()];
    }
}